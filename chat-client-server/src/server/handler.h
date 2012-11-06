/* *****************************************************************************
 * File: handler.h
 * ****************************************************************************/
/** handler.h
 * 
 * 	This file contains function declarations and structure definitions related
 * the the struct handler structure, which is used by the server.
 * 	Handler structures essentially store all relevant information about a
 * client connection and the thread assigned to "handle" the client's requests.
 * Handler structures also contain pointers to "previous" and "next" handler
 * structures so that each object can be loaded into a global handler list for
 * the server to keep track of.
 * 	The handler list is manipulated with functions declared in this file
 * prefix with hl_. They typically take a pointer to a struct handler_list, 
 * which indicates the start of the list.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Nov. 15, 2011
 */ 

#ifndef _HANDLER_H_
#define _HANDLER_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <shared/p_error.h>
#include <shared/p_mthread.h>
#include <shared/p_network.h>


/* *****************************************************************************
 * Global variables, mutexes, etc.
 * ****************************************************************************/
#define HBUF_SZ 1024		// Handler buffer size.


/* *****************************************************************************
 * Structure Definitions
 * ****************************************************************************/

/**
 * Handler structures are used to store information about different client
 * handlers.
 */
struct handler {
	pthread_mutex_t mx_life;	// Mutual exclusion for "alive" member.
	pthread_mutex_t mx_sock;	// Mutual exclusion for "sock" member.
	
	pthread_t tid;		// Thread ID of the thread associated with this handler.
	char ipstr[INET6_ADDRSTRLEN];
	int port;
	
	unsigned hid;	// Handler ID. 0 is default/unset.
	int alive;	// 1 if thread should not terminate; 0 if it should.
	char hbuf[HBUF_SZ];	// Holds received client messages.
	
	int sock;	// The socket this handler is connected on.
	struct sockaddr_storage addr;
	socklen_t addr_sz;
	
	// These are NULL if the object is not in a struct handler_list.
	struct handler *next;	// The next handler in the list.
	struct handler *prev;	// The previous handler in the list.
};


/**
 * List of handlers.
 */
struct handler_list {
	struct handler *head;
};


/* *****************************************************************************
 * Function Declarations
 * ****************************************************************************/
/**
 * Initializes a handler object.
 * @param h - The handler to initialize.
 * @return void - Nothing is returned. 
 */
void
init_handler(struct handler *);


/**
 * Allocates memory for a new handler object and initializes it.
 * @param void - No parameters.
 * @return struct handler * - Pointer to the new handler object.
 */
struct handler * 
new_handler(void);


/**
 * The routine executed by the client handler threads.
 * Note that the main thread does not join with handler threads; they are
 * set as detached upon creation.
 * @param arg - Argument to the routine, should be a struct handler* cast as a
 * 				void*.
 * @return void * - Returns NULL in this case.
 */
void *
handler_task(void *);


/**
 * Initializes a handler list - just sets the head to NULL.
 * @param l - The handler_list object to initialize.
 * @return void - Nothing is returned.
 */
void
init_handler_list(struct handler_list *);


/**
 * Add a handler to the head of a list.
 * @param l - The list structure to add the handler object to.
 * @param h - The handler object to add to the list.
 * @return int - Returns 0 for failure; 1 otherwise.
 */
int
hl_add_to_head(struct handler_list *, struct handler *);


/**
 * Remove a handler from the list. This function removes a handler from anywhere in the
 * list. Note that this does not free the memory of the handler - a pointer to
 * the removed handler is returned, and the caller can decide if they want to
 * free the memory.
 * @param l - The list to remove the handler from.
 * @param h - The handler object to remove from the list.
 * @return struct handler * - Pointer to the removed handler; NULL if failure.
 */
struct handler *
hl_remove(struct handler_list *, struct handler *); 


/**
 * Clears the list and frees memory. Not used at the moment, and not tested.
 * @param l - The list to clear.
 * @return int - Returns 0 for failure; 1 for success.
 */
int
hl_clear(struct handler_list *);





#endif
