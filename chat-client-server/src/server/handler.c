/* *****************************************************************************
 * File: handler.c
 * ****************************************************************************/
/** handler.c
 * 
 * 	Implementations of client handler related functions.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Nov. 15, 2011
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
	struct handler *iter = NULL;
	int still_alive = 1;	// Thread should keep executing.
	int rstatus = -1;	// Status of recv() attempt.
	
	// Used for select() waiting on the socket.
	fd_set sockfds;
	struct timeval timeout;
	int ret;
	
	
	MXLOCK(&this->mx_life);
	still_alive = this->alive;
	MXUNLOCK(&this->mx_life);
	
	while (still_alive) {
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
		
			// Go through the list of handlers and broadcast the last message.
			if (rstatus > 0) {
				MXLOCK(&mx_list);
			
				iter = g_list.head;
				while (iter != NULL) {
					MXLOCK(&iter->mx_sock);
					p_send_msg(&iter->sock, this->hbuf, sizeof(char)*strlen(this->hbuf));
					MXUNLOCK(&iter->mx_sock);
				
					iter = iter->next;
				}
			
				MXUNLOCK(&mx_list);
			}
		}
			
		// Terminate if the connection is closed.
		if (rstatus == 0) {
			still_alive = 0;
		}
		else {
			MXLOCK(&this->mx_life);
			still_alive = this->alive;
			MXUNLOCK(&this->mx_life);
		}
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
