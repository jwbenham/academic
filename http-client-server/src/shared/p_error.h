/* *****************************************************************************
 * File: p_error.h
 * ****************************************************************************/
/** p_error.h
 * 
 * 	Contains declarations and variables related to error handling.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Dec. 05, 2011
 */ 

#ifndef _P_ERROR_H_
#define _P_ERROR_H_
 
#include <errno.h>
#include <stdio.h> 
 

static const int P_EUSAGE = 1;	// Incorrect command line.
static const int P_EGAI = 2;	// Error with getaddrinfo()
static const int P_EOSOCK = 3;	// Could not open socket.
static const int P_ECONN = 4;	// Call to connect() failed.
static const int P_ECSOCK = 5; 	// Call to close() on a socket failed.
static const int P_ESENDF = 6;  // Failed to send message to server.
static const int P_ERECVF = 7;  // Failed to receive message from server.
static const int P_EREADER = 8; // Failed to create the reader thread.

static const int P_EIOTASK = 9; // Failed to start the IO thread.
static const int P_ETATTR = 10; // Error with a pthread_attr_t object.
static const int P_EBIND = 11; 	// Failed to bind to host port.
static const int P_ELISTEN = 12;	// Failed to mark port as passive.
static const int P_ESELECT = 13;	// Error with select().
static const int P_EACCEPT = 14;	// Error with accept().
static const int P_ETHREAD = 15;	// Error creating thread.

static const int P_EGERR = 16;	// General use error.

static const int P_EBADEXT = 17;	// Invalid path/file extension not found.
static const int P_EINVALEXT = 18;	// Invalid file extension.
static const int P_EINVALURL = 19;	// Invalid URL.
static const int P_EHTTP = 20;		// HTTP error.
static const int P_ERFILE = 21;		// Problem reading a file.
static const int P_EWFILE = 22;		// Problem writing a file.
 
 
/** p_perror - Print an error message.
 * @param code - the error code of the message to print.
 * @param extra - any extra/additional message to append. NULL is acceptable.
 * @return int - returns the return code for the error (usually the same as the
 * error code.
 */
int p_perror(int code, const char *extra);


#endif
