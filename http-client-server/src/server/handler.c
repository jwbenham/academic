/* *****************************************************************************
 * File: handler.c
 * ****************************************************************************/
/** handler.c
 * 
 * 	Implementations of client handler related functions.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Dec. 05, 2011
 */ 

#include "handler.h"


/* *****************************************************************************
 * Global variables, mutexes, etc.
 * ****************************************************************************/
extern pthread_mutex_t mx_list;
extern struct handler_list g_list;


/* *****************************************************************************
 * Function definitions.
 * ****************************************************************************/
void
init_handler(struct handler *h) 
{
	strcpy(h->ipstr, "Unknown IP");
	
	h->hid = 0;
	h->alive = 1;
	memset(h->hbuf, '\0', HBUF_SZ);
	
	pthread_mutex_init(&h->mx_life, NULL);
	pthread_mutex_init(&h->mx_sock, NULL);
	
	h->port = -1;
		
	h->next = NULL;
	h->prev = NULL;
}


struct handler * 
new_handler(void)
{
	struct handler *h = (struct handler *)malloc(sizeof(struct handler));
	init_handler(h);
	return h;
}


void *
handler_task(void *arg)
{
	struct handler *this = (struct handler *)arg;
	int still_alive = 1;	// Thread should keep executing.
	int rstatus = -1;	// Status of recv() attempt.
	
	struct p_http_req req_info;	// Store info. on the request.
	struct p_http_resp resp;	// The server's response.
	FILE *reqFile = NULL;
	
	// Used for select() waiting on the socket.
	fd_set sockfds;
	struct timeval timeout;
	int ret;
	
	
	MXLOCK(&this->mx_life);
	still_alive = this->alive;
	MXUNLOCK(&this->mx_life);
	
	if (still_alive) {
		memset(this->hbuf, '\0', HBUF_SZ);
		rstatus = -1;	// So we will not terminate accidentally.
		
		// Wait by calling select() on the socket.
		FD_ZERO(&sockfds);
		FD_SET(this->sock, &sockfds);
		timeout.tv_sec = 2;
		timeout.tv_usec = 0;
		select(this->sock + 1, &sockfds, NULL, NULL, &timeout);
		
		if (FD_ISSET(this->sock, &sockfds)) {		
			// Check for a message.
			MXLOCK(&this->mx_sock);
			rstatus = recv(this->sock, this->hbuf, sizeof(char)*HBUF_SZ-1, MSG_DONTWAIT);
			MXUNLOCK(&this->mx_sock);
		
			// Interpret the message and send an HTTP response.
			if (rstatus > 0) {
				int http_status = HTTP_BAD_REQUEST;

				// Determine what the status of the request is.
				if (p_parse_get_req(this->hbuf, req_info.path, req_info.file)) {
					
					if (p_valid_file(req_info.file) == 0) {
							
						if ((reqFile = fopen(req_info.path, "r")) != NULL) {
							
							http_status = HTTP_OK;
								
						} else {
							http_status = HTTP_NOT_FOUND;
						}
							
					} else {
						http_status = HTTP_UNSUPPORTED_MEDIA_TYPE;
					}
				
				} else {
					http_status = HTTP_BAD_REQUEST;
				}
				
				// Send a header based on the request.
				p_make_get_header(req_info.file, resp.header, http_status);
				
				MXLOCK(&this->mx_sock);
				ret = p_send_msg(&this->sock, resp.header, sizeof(char)*strlen(resp.header));
				MXUNLOCK(&this->mx_sock);
				
				// Send data if appropriate.
				if (http_status == HTTP_OK && ret != 0) {
					
					unsigned int bytes_read = 0;
						
					while ((still_alive) && (ret != 0) && (feof(reqFile) == 0)) {
						memset(resp.data, '\0', sizeof(resp.data));
							
						bytes_read = fread(resp.data, sizeof(char),
											sizeof(resp.data)/sizeof(char), 
											reqFile);
								
						
						MXLOCK(&this->mx_sock);
						ret = p_send_msg(&this->sock, resp.data, bytes_read);
						MXUNLOCK(&this->mx_sock);	
						
						MXLOCK(&this->mx_life);
						still_alive = this->alive;
						MXUNLOCK(&this->mx_life);					
					}
								
				}
			}
		}
	}
	
	
	if (ret == 0) {
		sstatus("An error occurred while sending a response:");
		printf("%s \n", strerror(errno));
	}

	if (reqFile != NULL) {
		fclose(reqFile);
	}
	
	MXLOCK(&mx_list);
	hl_remove(&g_list, this);	// Remove this one from the global list.
	MXUNLOCK(&mx_list);
	
	p_close_socket(&this->sock);	// Close the connection on this side.
	
	pthread_mutex_destroy(&this->mx_life);
	pthread_mutex_destroy(&this->mx_sock);
	
	sstatus("Client connection closed.");
	printf(" ID = %hi. ", this->hid);
	fflush(stdout);
	
	free((void*)this);
	pthread_exit(NULL);
}


void
init_handler_list(struct handler_list *l) 
{
	l->head = NULL;
}


int
hl_add_to_head(struct handler_list *l, struct handler *h) 
{
	if (h == NULL || l == NULL) {
		return 0;
	}
	
	if (l->head == NULL) {
		l->head = h;
		h->next = NULL;
		h->prev = NULL;
	}	
	
	else {
		h->next = l->head;
		h->prev = NULL;
		
		l->head->prev = h;
		
		l->head = h;
	}	
	
	return 1;
}


struct handler *
hl_remove(struct handler_list *l, struct handler *h) 
{
	struct handler *iter = NULL;
	struct handler *tmp = NULL;
	
	if (h == NULL || l == NULL) return NULL;
	
	if (l->head == h) {
		if (h->next == NULL) {
			l->head = NULL;
		}
		else {
			l->head = h->next;
			l->head->prev = NULL;
		}
	}
	
	else {
		if (h->next == NULL) {
			h->prev->next = NULL;
		}
		else {
			h->prev->next = h->next;
			h->next->prev = h->prev;
		}
	}
	
	h->next = h->prev = NULL;
	return h;
}


int
hl_clear(struct handler_list *l)
{
	struct handler *iter = NULL;
	struct handler *del = NULL;
	
	if (l == NULL) {
		return 0;
	}
	
	iter = l->head;
	while (iter != NULL) {
		del = iter;
		iter = iter->next;
		free((void *)del);		
	}
	
	return 1;
}
