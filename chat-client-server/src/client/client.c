/* *****************************************************************************
 * File: client.c
 * ****************************************************************************/
/** client.c
 *	This provides the implementation for the client side of the project.
 * 
 * 	A separate thread is spawned to read information from the remote server. The
 * main thread must join with the reader thread before it terminates. The main
 * thread sits in a loop reading input from the user and sending it to the 
 * remote server, while re-establishing the connection if necessary.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Nov. 15, 2011
 * 
 */
 
#include <stdlib.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

#include <shared/p_error.h>
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

// String the user types to quit the client.
const char *exit_msg = "*quit";	

#define INBUF_SZ 1024
char INBUF[INBUF_SZ];	// User input buffer.

#define RECVBUF_SZ 4096
char RECVBUF[RECVBUF_SZ];	// Message buffer from server.


/* *****************************************************************************
 * Function declarations.
 * ****************************************************************************/
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
 * reader thread checks the connection socket and outputs anything which comes
 * through.
 * @param arg - argument passed to the thread on creation - in this case it
 * 				should be the struct p_conn_params object representing the
 * 				connection with the remote server.
 * @return void - Nothing returned.
 */
void *
rdr_task(void *arg) ;


/**
 * Gets a line of input from the user. At this time, fgets() is used, which
 * includes the terminating '\n' of the input. In this implementation, the
 * client will not send a newline to the server unless one is expicitly 
 * input - i.e. the user simply presses the "return" key with no additional
 * input.
 * @return int - 0 if the input is equal to the exit message; nonzero otherwise.
 */
int 
get_input(void);


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
 * @param argv - Command line arguments; 1 should be the server; 2 should be the
 * 				port.
 * @return int - Returns 0 if no errors; nonzero otherwise.
 */
int 
main(int argc, char *argv[]) 
{	
	struct p_conn_params conn;	// Holds information for the connection.
	int errcode;	// Hold return error codes.
	int running = 1;	// 0 if program should exit.
	int unsent_msg = 0; // Indicates if last user message was not sent.
	pthread_t rdr_id;

	// Check proper usage.
	if (argc != 3) {
		return p_perror(P_EUSAGE, NULL);
	}

	// Initialize the connection.
	p_init_conn_params(&conn);
	conn.server = argv[1];
	conn.port = argv[2];	
	p_init_addrinfo(&conn.params, 0, AF_UNSPEC, SOCK_STREAM, IPPROTO_TCP);
	
	// Start up the reader.
	g_rdr = 1;
	if (pthread_create(&rdr_id, NULL, &rdr_task, (void *) &conn)) {
		return p_perror(P_EREADER, NULL);
	}
	
	
	// Main program loop.
	pstatus("Client program. Enter *quit to quit.");
	
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
		
		// Get a line of input from the user, unless the previous message was
		// unsent (e.g. because of disconnect).
		fflush(stdout);
		if (unsent_msg) {
			pstatus("Last message was unsent. Now sending:");
			printf("\n%s\n", INBUF);
			unsent_msg = 0;
		}
		else {
			printf(">>");
			running = get_input();
		}	
	
		// Continue and send if the user had not chosen to quit.
		if (running) {
			
			// Check connection.
			errcode = p_test_conn(conn.socket);
			if (errcode == 0) {
				pstatus("The server closed the connection.\n");
				sync_set_active(&conn, &mx_active, 0);
				p_close_socket(&conn.socket);
				unsent_msg = 1;
			}
		
			// Send the message if the connection is ok.
			if (sync_get_active(&conn, &mx_active)) { 
				MXLOCK(&mx_socket);

				if (p_send_msg(&conn.socket, INBUF, sizeof(char)*strlen(INBUF)) == 0) {
					p_perror(P_ESENDF, strerror(errno));
					running = 0;
				} 
			
				MXUNLOCK(&mx_socket);
			}
		}		
			
	} while(running);
	
	
	// Close the socket and join with the reader thread.
	clean_exit(0, &conn, &rdr_id);
}


/* *****************************************************************************
 * Function definitions.
 * ****************************************************************************/
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
	int alive;	// 1 if the reader should run; 0 if it should end.
	int active;	// 1 if the connection is active (conn->active == 1)
	int rstatus;
	struct p_conn_params *conn = (struct p_conn_params *)arg;
	char testbuf;

	fd_set sockfds;		// Used for select() when waiting for connections.
	struct timeval timeout;
	int ret;
	
	struct timeval active_wait;
	
	
	// When the reader first starts, check if it should run.
	MXLOCK(&mx_rdr_life);
	alive = g_rdr;
	MXUNLOCK(&mx_rdr_life);

	// While alive, check the socket for messages and output them.
	while (alive) {
		memset(RECVBUF, '\0', sizeof(char)*RECVBUF_SZ);

		// Wait for the connection to be active.
		active = sync_get_active(conn, &mx_active);
		
		while (active == 0) {
			// Wait for a little bit, then check again for an active connection.
			active_wait.tv_sec = 1;
			active_wait.tv_usec = 0;
			select(0, NULL, NULL, NULL, &active_wait);
			
			active = sync_get_active(conn, &mx_active);
		}
		

		// Use select() to wait for an incoming connection.		
		FD_ZERO(&sockfds);
		FD_SET(conn->socket, &sockfds);	
		timeout.tv_sec = 1;
		timeout.tv_usec = 10000;
		select(conn->socket + 1, &sockfds, NULL, NULL, &timeout);
		
		if (FD_ISSET(conn->socket, &sockfds)) {	
			MXLOCK(&mx_socket);
	
			// Set max bytes received to RECVBUF_SZ - 1 so we have room to add \0.
			rstatus = recv(conn->socket, RECVBUF, 
						sizeof(char) * RECVBUF_SZ - 1, MSG_DONTWAIT);
						
			MXUNLOCK(&mx_socket);		
		
			// Print if there was a message.
			if (rstatus > 0) {
				RECVBUF[rstatus] = '\0';	
				printf("\n%s\n>>", RECVBUF); 
			}
			fflush(stdout);
		}			

		// Check if it should continue running.
		MXLOCK(&mx_rdr_life);
		alive = g_rdr;
		MXUNLOCK(&mx_rdr_life);
	}
	
	pthread_exit(0);
}


int 
get_input(void) 
{
	memset(INBUF, '\0', INBUF_SZ);
	
	fgets(INBUF, INBUF_SZ, stdin);	
	
	/* If the input is not a single character (which would be a newline, since 
	 * we are using fgets()), strip the ending newline from the input.
	 * This is so that a newline is only sent if the user explicitly inputs one.
	 */
	int len = strlen(INBUF);
	if (len > 1) {
		INBUF[len-1] = '\0';
	}
	
	return strncmp(INBUF, exit_msg, 5); 
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

