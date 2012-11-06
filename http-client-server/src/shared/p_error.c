/* *****************************************************************************
 * File: p_error.c
 * ****************************************************************************/
/** p_error.c
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Dec. 05, 2011
 */
 
#include "p_error.h"

/* *****************************************************************************
 * Functions and definitions with file-scope.
 * ****************************************************************************/

// Holds the error messages.
static const char *messages[] =  { "",
					  "usage: httpget <URL> <port>",
					  "getaddrinfo() returned an error",
					  "failed to open a socket for the server",
					  "failed to connect to the server",
					  "failed to close connection socket - may not have been open",
					  "the message could not be sent to the server",
					  "failed to receive a message from the server",
					  "failed to create the reader thread", 
					  "failed to create the server IO thread",
					  "error occurred with thread attributes",
					  "failed to bind to host port",
					  "an error occurred while trying to listen on a local port",
					  "an error resulted from a call to select()",
					  "an error occurred while accepting a connection",
					  "there was an error creating a new thread",
					  "an error occurred",
					  "no file extension was found in the given path",
					  "the provided file extension is not supported",
					  "the provided URL was invalid",
					  "HTTP error",
					  "error reading from file",
					  "error writing to file" };



/* *****************************************************************************
 * Functions with project-wide scope.
 * ****************************************************************************/

int 
p_perror(int code, const char *extra) 
{	
	static long count = 0;
	fprintf(stderr, "\n**Error[%li] - %s\n", count++, messages[code]);
	
	if (extra != NULL) {
		fprintf(stderr, "\tInformation: %s\n", extra);
	}
	
	fflush(stdout);
	
	return code;
}

