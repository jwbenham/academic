/* *****************************************************************************
 * File: httpget.c
 * ****************************************************************************/
/** httpget.c
 *	This provides the implementation for the HTTP client side of the project.
 * 
 * 	A separate thread is spawned to read information from the remote server. The
 * main thread must join with the reader thread before it terminates. The main
 * thread sits in a loop attempting to send the GET request. Once the request
 * has been sent successfully, the loop ends and the main thread waits for the
 * reader thread to join with it.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Dec. 05, 2011
 * 
 */
 
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

#include <shared/p_error.h>
#include <shared/p_http.h>
#include <shared/p_mthread.h>
#include <shared/p_network.h>


/* *****************************************************************************
 * Global variables, mutexes, etc.
 * ****************************************************************************/
pthread_mutex_t mx_rdr_life = PTHREAD_MUTEX_INITIALIZER;
int g_rdr = 0;	// Determines if the reader is running.
	
// Mutual exclusion for the connection socket.
pthread_mutex_t mx_socket = PTHREAD_MUTEX_INITIALIZER;

// Mutual exclusion for the connection's activity indicator variable.
pthread_mutex_t mx_active = PTHREAD_MUTEX_INITIALIZER;

#define RECVBUF_SZ 4096
char RECVBUF[RECVBUF_SZ];	// Message buffer from server.

struct p_http_req g_req;	// Holds information about the http request.

/* *****************************************************************************
 * Function declarations.
 * ****************************************************************************/
/**
 * Does some basic checking to make sure the user provided input is correct and
 * valid.
 * @param argc - The number of command line arguments.
 * @param url - The URL to check for correctness.
 * @return int - Returns zero if no error; nonzero otherwise.
 */
int 
check_cl_args(int argc, char *url);
 

/**
 * Checks that a URL is valid.
 * @param url - The string with the URL to check.
 * @return int - Returns 0 if no errors; nonzero error code otherwise.
 */ 
int
is_valid_url(char *url);

 
/**
 * Simple function to check if a URL is in the basic correct format; it checks
 * that the string begins with "http://", and that it has a "/" at some point to
 * designate the start of a file path.
 * @param url - The string to check.
 * @return int - Returns 0 for no errors; returns 1 for no "http://"; returns 2
 * 				for no "/" indicating resource path.
 */
int 
check_url_format(char *url);
 
 
 /** 
 * Attempts to shut down the program cleanly - destroying mutexes, freeing 
 * memory, joining with threads, etc.
 * @param exit_code - an exit code indicating the state of the program.
 * @param cp - the connection parameters - used to close the connection.
 * @param t - the ID of the reader thread - used to join.
 * @return void - Nothing returned.
 */
void 
clean_exit(int exit_code, struct p_conn_params *cp, pthread_t *t);


/**
 * 	This is the function assigned to be executed by the reader thread. The 
 * reader thread checks the connection socket and outputs HTTP headers, then
 * writes any available data to a file specified by the user on the command
 * line.
 * @param arg - argument passed to the thread on creation - in this case it
 * 				should be the struct p_conn_params object representing the
 * 				connection with the remote server.
 * @return void - Nothing returned.
 */
void *
rdr_task(void *arg) ;


/**
 * Simple utility function to print a status update.
 * @param msg - status message to print.
 * @return void - nothing is returned.
 */
void
pstatus(char *msg);


/**
 * Provides mutually exclusive retrieval for the connection's activity indicator 
 * variable.
 * @param cp - the connection parameters object the connection is made on.
 * @param mx - an activity mutex for the parameter connection.
 * @return int - returns cp->active
 */
int
sync_get_active(struct p_conn_params *cp, pthread_mutex_t *mx);


/** 
 * Provides mutually exclusive modification of the connection's activity 
 * indicator variable.
 * @param cp - the connection parameters object the connection is made on.
 * @param mx - an activity mutex for the parameter connection.
 * @param val - the function performs cp->active = val;
 * @return void - Nothing is returned.
 */
void
sync_set_active(struct p_conn_params *cp, pthread_mutex_t *mx, int val);



/* *****************************************************************************
 * Main 
 * ****************************************************************************/
/** 
 * Collects user input and performs the main program loop.
 * @param argc - Number of arguments; should be 3.
 * @param argv - Command line arguments; 1 should be the URL; 2 should be the
 * 				port.
 * @return int - Returns 0 if no errors; nonzero otherwise.
 */
int 
main(int argc, char *argv[]) 
{	
	struct p_conn_params conn;	// Holds information for the connection.
	int errcode;	// Hold return error codes.
	int running = 1;	// 0 if program should exit.
	int unsent_msg = 1; // Indicates if last user message was not sent. It is
						// initialized to 1 so that we send the command line
						// request.
	pthread_t rdr_id;


	// Check that the command line argument is valid.
	if ((errcode = check_cl_args(argc, argv[1])) != 0) {
		return errcode;
	}
	
	// Parse the hostname and resource path from the provided URL.
	p_parse_url(argv[1], &g_req);
	p_make_get_req(&g_req);
	

	// Initialize the connection.
	p_init_conn_params(&conn);
	conn.server = g_req.host;
	conn.port = argv[2];	
	p_init_addrinfo(&conn.params, 0, AF_UNSPEC, SOCK_STREAM, IPPROTO_TCP);
	
	// Start up the reader.
	g_rdr = 1;
	if (pthread_create(&rdr_id, NULL, &rdr_task, (void *) &conn)) {
		return p_perror(P_EREADER, NULL);
	}
	
	
	do {
		// Reconnect if necessary.
		if (sync_get_active(&conn, &mx_active) != 1) {
			pstatus("Attempting to establish connection..");
			
			MXLOCK(&mx_socket);
			MXLOCK(&mx_active);
			
			errcode = p_connect(&conn);	// Try to connect.
			
			MXUNLOCK(&mx_active);
			MXUNLOCK(&mx_socket);

			if (errcode) {	// Connection failed.
				p_perror(errcode, "In main loop, after (re)connect block");
				clean_exit(errcode, &conn, &rdr_id);
			}
			
			pstatus("Connection opened.\n");
		}

		// Check connection.
		errcode = p_test_conn(conn.socket);
		if (errcode == 0) {
			pstatus("The server closed the connection.\n");
			
			sync_set_active(&conn, &mx_active, 0);	// Connection is inactive.
			
			p_close_socket(&conn.socket);	// Close connection on our end.
		
			unsent_msg = 1;	// Continue to loop.
		}
		
		// Send the message if the connection is ok.
		if (sync_get_active(&conn, &mx_active)) { 
			MXLOCK(&mx_socket);

			if (p_send_msg(&conn.socket, g_req.req, sizeof(char)*strlen(g_req.req)) == 0) {
				p_perror(P_ESENDF, strerror(errno));
				running = 0;
			} else {
				unsent_msg = 0;	// Message sent, we can break the loop.
			} 
		
			MXUNLOCK(&mx_socket);
		}
	
	} while(running && (unsent_msg == 1));
	
	// Close the socket and join with the reader thread.
	clean_exit(0, &conn, &rdr_id);
}


/* *****************************************************************************
 * Function definitions.
 * ****************************************************************************/
int 
check_cl_args(int argc, char *url)
{
	int errcode;
	
	// Check proper command line arguments.	
	if (argc != 3) {
		return p_perror(P_EUSAGE, NULL);
	}
	
	// Check proper URL.
	errcode = is_valid_url(url);
	if (errcode != 0) {
		return errcode;
	}	
	
	// Return 0 for no errors.
	return 0;
}


int
is_valid_url(char *url)
{
	int errcode;
	
	// Check that the URL has a valid format.
	if ((errcode = check_url_format(url)) != 0) {
		if (errcode == 1) {
			return p_perror(P_EINVALURL, "URL must begin with 'http://'");
		} else if (errcode == 2) {
			return p_perror(P_EINVALURL, "no resource path found in the URL");
		}
	}
	
	
	// Check that the file extension is supported.
	if ((errcode = p_valid_file(url)) != 0) {
		return p_perror(errcode, NULL);
	}	
	
	return 0;
}


int 
check_url_format(char *url)
{
	// Check for "http://" at the beginning of the URL.
	if (strncmp(url, "http://", 7) != 0) {
		return 1;
	}
	
	// Check for a path specification "/"
	char *p = url + 7;	// +7 since we know it starts with "http://"	
	
	if (strstr(p, "/") == NULL) {
		return 2;
	}	
		
	return 0;
}


void 
clean_exit(int exit_code, struct p_conn_params *cp, pthread_t *t) 
{
	MXLOCK(&mx_rdr_life);
	g_rdr = 0;
	MXUNLOCK(&mx_rdr_life);
	
	pthread_join(*t, NULL);
	pstatus("Reader thread shutdown.");
	
	if (cp->active) {
		p_close_socket(&cp->socket);
	}
	pstatus("Connection closed.");
	
	// Exit successfully.
	printf("\n");
	exit(exit_code);
}


void *
rdr_task(void *arg) 
{
	int alive = 1;	// 1 if the reader should run; 0 if it should end.
	int active;	// 1 if the connection is active (conn->active == 1)
	int rstatus;
	struct p_conn_params *conn = (struct p_conn_params *)arg;

	fd_set sockfds;		// Used for select() when waiting for connections.
	struct timeval timeout;
	int ret;
	
	struct timeval active_wait;
	
	struct p_http_resp resp;
	int header_read = 0;
	int timeouts = 0;
	int write_elements;	// Number of elements for fwrite to write out.
	int written;	// Number of elements fwrite wrote.
	int success = 1;


	// Wait for the connection to be active.
	active = sync_get_active(conn, &mx_active);
		
	while ((alive) && (active == 0)) {		
		// Wait for a little bit, then check again for an active connection.
		active_wait.tv_sec = 1;
		active_wait.tv_usec = 0;
		select(0, NULL, NULL, NULL, &active_wait);
			
		active = sync_get_active(conn, &mx_active);
		
		// Check if we should be stopping for some reason.
		MXLOCK(&mx_rdr_life);
		alive = g_rdr;
		MXUNLOCK(&mx_rdr_life);			
	}
		
		
	// Overwrite any previous file with the same name. Then open for appending.
	FILE *respFile = fopen(g_req.file, "w");
	fclose(respFile);
	respFile = fopen(g_req.file, "a");
	
	do {
		memset(RECVBUF, '\0', sizeof(char)*RECVBUF_SZ);		
		
		// Use select() to wait for an incoming connection.		
		FD_ZERO(&sockfds);
		FD_SET(conn->socket, &sockfds);	
		timeout.tv_sec = 1;
		timeout.tv_usec = 10000;
		select(conn->socket + 1, &sockfds, NULL, NULL, &timeout);
		
		if (FD_ISSET(conn->socket, &sockfds)) {	
			
			// Set max bytes received to RECVBUF_SZ - 1 so we have room to add \0.
			MXLOCK(&mx_socket);
			rstatus = recv(conn->socket, RECVBUF, 
						sizeof(char) * RECVBUF_SZ - 1, MSG_DONTWAIT);
			MXUNLOCK(&mx_socket);		
		
			// Handle the message if there was one.
			if (rstatus > 0) {		
				timeouts = 0;	// Reset the timeouts.	
					
				// Print out the header if we have not already.
				if (header_read == 0) {
					
					if (p_parse_http_header(RECVBUF, resp.header) == 0) {
						p_perror(P_EHTTP, "Failed to parse the HTTP header.");
						success = 0;
						break;
					}			
					
					printf("\n%s\n", resp.header); 					
					fflush(stdout);
					header_read = 1;					
					
					// Write anything leftover to the file.					
					char *data = p_find_data_section(RECVBUF);
					int num = rstatus - sizeof(char) * (strlen(resp.header) + 4);
					
					written = fwrite(data, sizeof(char), num/sizeof(char), respFile);
					
					if (written != num/sizeof(char)) {
						p_perror(P_EWFILE, "A problem occurred while writing to the file.");
						success = 0;
						break;
					}
					
				} else {						
								
					// Print out data to file.
					write_elements = rstatus/sizeof(char);
					written = fwrite(RECVBUF, sizeof(char), write_elements, respFile);			
				
					if (write_elements != written) {
						p_perror(P_EWFILE, "A problem occurred while writing to the file.");
						success = 0;
						break;
					}
				
				}
				
			} else {
				timeouts++;
			}
		}
		
	} while (rstatus != 0 && timeouts < 20);			


	fclose(respFile);	

	if (success == 0 || timeouts >= 20) {
		remove(g_req.file);
	}

	pthread_exit(0);
}


void
pstatus(char *msg) 
{
	static long count = 0;	
	printf("\n[Client: %li] %s", count++, msg);
	fflush(stdout);
}


int
sync_get_active(struct p_conn_params *cp, pthread_mutex_t *mx)
{
	int ret;
	
	MXLOCK(mx);
	ret = cp->active;
	MXUNLOCK(mx);
	
	return ret;
}


void
sync_set_active(struct p_conn_params *cp, pthread_mutex_t *mx, int val)
{
	MXLOCK(mx);
	cp->active = val;	
	MXUNLOCK(mx);
}

