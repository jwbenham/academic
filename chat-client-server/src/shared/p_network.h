/* *****************************************************************************
 * File: p_network.h
 * ****************************************************************************/
/** p_network.h
 * 
 * 	This file contains functions and structure definitions related to 
 * networking tasks/routines that need to be executed by both the client and
 * the server - such as finding a valid address in a list of results, or 
 * converting a network address to presentation format.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Nov. 15, 2011
 */ 

#ifndef _P_NETWORK_H_
#define _P_NETWORK_H_

#include <ifaddrs.h>
#include <netdb.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "p_error.h"


/* *****************************************************************************
 * Macros, Typedefs, etc.
 * ****************************************************************************/
// Arbitrarily large number for doing conversions between struct sockaddr 
// internet addresses.
#define SA_SZ 128


/* *****************************************************************************
 * Public structure definitions
 * ****************************************************************************/
struct p_conn_params {
	char *server;
	char *port;
	
	struct addrinfo params;	// Parameters for the connection to create.
	struct addrinfo *server_info;	// Results of a lookup on a domain.
	
	struct addrinfo *open;	// Parameters we used to create a socket. 
	int socket;
	int active;		
};



/* *****************************************************************************
 * Public function declarations
 * ****************************************************************************/

/** 
 * Initializes a connection parameters structure.
 * @param cp - The structure to initialize.
 * @return void - Nothing returned.
 */
void 
p_init_conn_params(struct p_conn_params *cp);


/**
 * Initializes an addrinfo structure.
 * @param ai - Pointer to the addrinfo to initialize.
 * @param flags - Value for ai_flags.
 * @param fam - Value for ai_family.
 * @param sock - Value for ai_socktype.
 * @param prot - Value for ai_protocol.
 * @return void - Nothing returned.
 */
void 
p_init_addrinfo(struct addrinfo *ai, int flags, int fam, int sock, int prot);

	
/**
 * Attempts to create a socket using the search results from a domain lookup.
 * @param cp - Pointer to the connection parameters object with the results.
 * @return int - Returns 1 if a socket was created; 0 otherwise.
 */	
int 
p_find_socket(struct p_conn_params *cp);


/**
 * Closes a socket, with error checking.
 * @param sock - Pointer to the socket file descriptor to close.
 * @return int - Returns 0 if no errors; returns one of the error numbers 
 * 				defined in "error.h" otherwise.
 */
int
p_close_socket(int *sock);


/**
 * Performs a lookup on a given domain and port, creates a socket using the
 * lookup results, and attempts to create a connection on the socket.
 * @param cp - Pointer to the connection parameters to use.
 * @return int - Returns 0 if no errors; returns one of the error numbers 
 * 				defined in "error.h" otherwise.
 */
int 
p_connect(struct p_conn_params *cp);


/**
 * Attempts to send the entirety of the msg parameter.
 * @param sock - Pointer to the socket to send the message over.
 * @param msg - The message to send.
 * @param sz - How many bytes of the message to send.
 * @return - Returns 0 on error (check errno); returns 1 otherwise.
 */
int 
p_send_msg(int *sock, char *msg, int sz);


/**
 * Tests a connection to see if it is still open.
 * @param sock - the socket the connection is made on.
 * @return - Returns 1 if the connection is open; returns 0 if the connection is
 * 			closed; returns less than zero if error.
 */
int
p_test_conn(int sock);


/** 
 * Gets address information from a socket.
 * @param sockfd - The target socket file descriptor.
 * @param sa - The sockaddr structure to store the resulting address in.
 * @param sa_len - The size of the sa parameter.
 * @return int - Returns 0 if sa_len was not large enough; returns less than 0
 *				on error; returns greater than 0 for success.
 */
int
p_get_sock_name(int sockfd, struct sockaddr *sa, unsigned sa_len);


/**
 *	Extracts a port number from a struct sockaddr.
 * @param sa - The sockaddr to extract the port number from.
 * @return int - Returns -1 for error; greater than 0 otherwise (the port #).
 */
int
p_port_from_sa(struct sockaddr *sa);


/**
 *	Takes a pointer to a socket address, extracts the IP address in presentation
 * format, and stores it in dst.
 * @param sa - The socket address to take the network address from.
 * @param dst - Where the resulting presentation format address should be 
 *				stored. This should be of length at least INET6_ADDRSTRLEN.
 * @return char * - Returns dst if successful; NULL otherwise.
 */
char *
p_ip_from_sa(struct sockaddr *sa, char *dst);


/**
 * Prints out a list of the local machines interface addresses. This could be
 * used by a server user to find their external IP address.
 * @param void - No arguments.
 * @return void - Returns nothing.
 */
void
p_print_if_addrs(void);



#endif
