/* *****************************************************************************
 * File: server.c
 * ****************************************************************************/
/** server.c
 *	This provides the implementation for the server side of the project. The 
 * main thread spawns an additonal I/O thread, which wait for the user to enter
 * the exit message, indicating the server should shutdown.
 * 	The main thread sets up threading and connection parameters, then sits in
 * a loop listening for incoming connection atttempts. For each connection, a 
 * new struct handler object is created to hold information about the client,
 * and a new thread is spawned with its handler object as its argument. See 
 * handler.h for more information on handler structures.
 * 	When the server terminates, it closes its socket and cleans up other data
 * structures. It will alert all active client handlers to shutdown, but it
 * does not wait indefinitely for this to happen; only 5 seconds.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Nov. 15, 2011
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
#include <shared/p_mthread.h>
#include <shared/p_network.h>

#include "handler.h"


/* *****************************************************************************
 * Global variables, mutexes, etc.
 * ****************************************************************************/

/* Mutex for the global handler list. */
pthread_mutex_t mx_list = PTHREAD_MUTEX_INITIALIZER;	

/* The global handler list. This is a linked list of struct handler* objects.
 * When new client connections are made, a memory for a handler object is
 * allocated, the object is added to this list, and a new thread is spawned with
 * the handler object as an argument. When the connection closes, the object is
 * removed from the list and the memory is freed.
 */
struct handler_list g_list;	
							

pthread_mutex_t mx_exit_var = PTHREAD_MUTEX_INITIALIZER;
int g_exit_var = 0;	// 0 - server should terminate; nonzero - keep running

const char *EXIT_MSG = "*quit";
const char *DEFAULT_PORT = "19000";
const int MAX_CLIENTS = 8;
const int SERV_TO_SEC = 3;	// How long the server waits for an incoming
							// connection before it checks for exit condition.



/* *****************************************************************************
 * Function declarations.
 * ****************************************************************************/
/**
 * The routine executed by the I/O thread. This essentially waits for the user
 * to enter the exit string, at which point it flips the global variable
 * g_exit_var to 1, indicating the server should terminate. 
 * @param arg - The argument to the IO thread - NULL for now.
 * @return void * - No return value.
 */
void *
io_task(void *);


/**
 * Attempts to collect local host address information and bind to a local port.
 * @param serv - the server connection set-up parameters.
 * @param port - the port to attempt to bind to.
 * @return - Returns 0 for succes; -1 to indicate no failure, but the argument
 * 			port was already in use; some value > 0 indicates error.
 */
int
setup_server(struct p_conn_params *serv, char *port) ;


/**
 * Attempts to exit cleanly - i.e. makes sure that all threads are cleaned up,
 * memory freed, etc.
 * @param exit_code - An exit code to return to the terminal.
 * @param attr - The pthread_attr_t object used to initialize new threads; it
 * 				should be destroyed. #TODO: This could be done better..
 * @param ssock - The socket the server was listening on. This will be closed.
 * @return void - Nothing is returned; this function calls exit(exit_code).
 */
void 
clean_exit(int exit_code, pthread_attr_t *attr, int ssock);


/**
 * Simple utility function to print a status update.
 * @param msg - A status message to print.
 * @return void - Nothing is returned.
 */
void 
sstatus(char *msg);



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
	int server_exit;	// = 1 if the server should terminate.
	int ret;			// General purpose return value storage.
	struct sockaddr *sa = malloc(SA_SZ);	// Used to convert network addresses.
	
	pthread_t io_tid;	// Thread ID of the user input thread.
	
	unsigned next_hid = 1;			// Used to assign handler IDs.
	pthread_attr_t h_attr;			// Handler thread attributes.
	struct handler *new_h = NULL;	// Used to allocate memory for new handlers.
	init_handler_list(&g_list);		// List of active client handlers (global).
	
	struct p_conn_params server;	// Server connection parameters.
	char s_ipstr[INET6_ADDRSTRLEN];	// Server address.
	int s_port;						// Server port - int.
	char str_port[10];
	
	fd_set listenfds;			// 1-element set containing the listening socket.
	struct timeval timeout;	// Timeout before server checks if it should exit.
	
	
	// Set up the handler thread attribute to be in detached state - we do not 
	// need the main thread to join with them; as soon as their client has
	// disconnected they can have their resources reallocated.
	ret = pthread_attr_init(&h_attr);
	if (ret) {
		ret = p_perror(P_ETATTR, strerror(ret));
		clean_exit(ret, &h_attr, server.socket);
	}	
	
	ret = pthread_attr_setdetachstate(&h_attr, PTHREAD_CREATE_DETACHED);
	if (ret) {
		ret = p_perror(P_ETATTR, strerror(ret));
		clean_exit(ret, &h_attr, server.socket);
	}
	
	
	// Start up the IO thread.
	if (pthread_create(&io_tid, &h_attr, &io_task, NULL)) {
		return p_perror(P_EIOTASK, NULL);
	}
	sstatus("Server input started. Enter *quit to exit.\n");	
	
	// Print the list of interface addresses for this machine.
	p_print_if_addrs();
	
	// Get the port number the user wishes to listen on, if available.
	if (argc == 2) {
		strncpy(str_port, argv[1], 6);
		str_port[5] = '\0';
	}
	else {
		strncpy(str_port, DEFAULT_PORT, 6);
	}
		
	
	// Set up the server socket information
	p_init_conn_params(&server);
	p_init_addrinfo(&server.params, AI_PASSIVE, AF_UNSPEC, SOCK_STREAM, IPPROTO_TCP);
	
	ret = setup_server(&server, str_port);
	
	if (ret > 0) {
		clean_exit(ret, &h_attr, server.socket);
	}	
	
	else if (ret == -1) {
		sstatus("The attempted port number was in use: ");
		printf("%s", str_port);
				
		// Loop until we get a usable port..
		int p = atoi(str_port);
		if (p < 1024) p = 1024;
	
		while (ret == -1 && p < 65535) {
			p++;
			sprintf(str_port, "%d", p);
		
			freeaddrinfo(server.server_info);
			
			sstatus("Attempting to bind to port: ");
			printf("%s", str_port);
			
			ret = setup_server(&server, str_port);
			
			if (ret > 0) {
				clean_exit(ret, &h_attr, server.socket);
			}
		}
		
		if (p == 65535) {
			sstatus("There are no ports available to bind to.");
			clean_exit(ret, &h_attr, server.socket);
		}
	}

	freeaddrinfo(server.server_info);

	
	// Alert user to port.
	sstatus("Server set up and listening.");
	if (p_get_sock_name(server.socket, sa, SA_SZ) > 0) {
		printf(" Port = %i\n", p_port_from_sa(sa));
	}
	
		
	MXLOCK(&mx_exit_var);
	server_exit = g_exit_var;
	MXUNLOCK(&mx_exit_var);

	// Main server client-handling loop.
	while (server_exit == 0) {
		
		// Wait for incoming connections.
		FD_ZERO(&listenfds);
		FD_SET(server.socket, &listenfds);
		timeout.tv_sec = SERV_TO_SEC;
		timeout.tv_usec = 0;
		
		ret = select(server.socket + 1, &listenfds, NULL, NULL, &timeout);
		
		if (ret == -1) {
			ret = p_perror(P_ESELECT, strerror(errno));
			clean_exit(ret, &h_attr, server.socket);	
		}
		
		// There is an incoming connection.
		else if (FD_ISSET(server.socket, &listenfds)) {	
			sstatus("Incoming connection. ");
			
			new_h = new_handler();			
			new_h->sock = accept(server.socket, (struct sockaddr *)&new_h->addr,
								 &new_h->addr_sz);
					
			
			if (new_h->sock == -1) {	// accept() failed
				ret = p_perror(P_EACCEPT, strerror(errno));
				clean_exit(ret, &h_attr, server.socket);
			}
			
			else {				
				// Get some info on the new client and assign an ID.
				if (p_get_sock_name(new_h->sock, sa, SA_SZ) > 0) {
					new_h->port = p_port_from_sa(sa);
					p_ip_from_sa(sa, new_h->ipstr);
				}				
				new_h->hid = next_hid++;
				
				// Spawn a new handler.
				ret = pthread_create(&new_h->tid, &h_attr, &handler_task, 
									 (void *)new_h);
				if (ret == -1) {
					ret = p_perror(P_ETHREAD, strerror(ret));
					clean_exit(ret, &h_attr, server.socket);
				}
				
				sstatus("New connection established. ID = ");
				printf("%hi. ", new_h->hid);
				fflush(stdout);
				
				if (new_h->ipstr != NULL) printf("%s", new_h->ipstr);
				printf(" : %i\n", new_h->port);
				fflush(stdout);
				
				// Add the handler information to the global list.
				MXLOCK(&mx_list);
				hl_add_to_head(&g_list, new_h);
				MXUNLOCK(&mx_list);
			}			
		}
		
		
		MXLOCK(&mx_exit_var);
		server_exit = g_exit_var;
		MXUNLOCK(&mx_exit_var);
	}
	
	
	// Clean up threads, data structures, etc., and exit.
	sstatus("Server shutting down.. ");
	clean_exit(0, &h_attr, server.socket);		
}


/* *****************************************************************************
 * Function definitions - see declarations for usage info.
 * ****************************************************************************/
void *
io_task(void *arg) 
{
	int alive = 1;
	char input[25];
	
	while (alive) {
		fgets(input, 25, stdin);
		fflush(stdout);
		
		if (strncmp(input, EXIT_MSG, 5) == 0) {
			MXLOCK(&mx_exit_var);
			g_exit_var = 1;
			MXUNLOCK(&mx_exit_var);
			
			alive = 0;
		}
	}		
	
	sstatus("Input thread terminated. ");
	pthread_exit(NULL);
}


// Returns -1 for port-in-use; 0 for success; 0 < for failure.
int
setup_server(struct p_conn_params *serv, char *port) 
{
	int ret;
	
	ret = getaddrinfo(NULL, port, &serv->params, &serv->server_info);
	if (ret != 0) {
		ret = p_perror(P_EGAI, gai_strerror(ret));
		return ret;
	}
	
	if (p_find_socket(serv) == 0) {
		ret = p_perror(P_EOSOCK, "server failed to open a socket");
		freeaddrinfo(serv->server_info);
		return ret;
	}
	
	ret = bind(serv->socket, serv->open->ai_addr, serv->open->ai_addrlen);
	if (ret == -1) {
		if (errno == EADDRINUSE) {
			return -1;
		}
		else {		
			ret = p_perror(P_EBIND, strerror(errno));
			return ret;
		}
	}
	
	if (listen(serv->socket, MAX_CLIENTS) == -1) {
		ret = p_perror(P_ELISTEN, strerror(errno));
		return ret;
	}
	
	return 0;
}


void 
clean_exit(int exit_code, pthread_attr_t *attr, int ssock)
{
	struct handler *iter = NULL;
	struct timeval timeout;
	timeout.tv_sec = 5;
	timeout.tv_usec = 0;

	sstatus("Clearing client handlers. ");	
	MXLOCK(&mx_list);
	iter = g_list.head;
	while (iter != NULL) {
		MXLOCK(&iter->mx_life);
		iter->alive = 0;
		MXUNLOCK(&iter->mx_life);
		iter = iter->next;
	}
	MXUNLOCK(&mx_list);
	
	select(0, NULL, NULL, NULL, &timeout);
	
	pthread_attr_destroy(attr);
	
	p_close_socket(&ssock);
	
	sstatus("Server terminated.\n");
	exit(exit_code);
}


void
sstatus(char *msg) {
	static long count = 0;	
	printf("\n[Server: %li] %s", count++, msg);
	fflush(stdout);
}









