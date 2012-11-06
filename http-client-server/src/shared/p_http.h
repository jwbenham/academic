/* *****************************************************************************
 * File: p_http.h
 * ****************************************************************************/
/** p_http.h
 * 
 *	This is the specification file for HTTP related functions and structures.
 * Functions contained in this file are concerned with validation, parsing, and
 * general manipulation of HTTP headers, data, etc. Documentation is in this 
 * file.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Dec. 05, 2011
 */ 


#ifndef _P_HTTP_H_
#define _P_HTTP_H_

#include <shared/p_error.h>

#include <string.h>


static const int HTTP_OK = 200;
static const int HTTP_BAD_REQUEST = 400;
static const int HTTP_NOT_FOUND = 404;
static const int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;

struct p_http_req {
	char host[256];
	char path[1024];
	char file[256];	
	char req[2048];
};

struct p_http_resp {
	char header[2048];
	char data[4096];
};


/**
 * Checks to see if a file extension in a path is supported by the program.
 * @param path - The path containing the file to check.
 * @return int - Returns 0 if valid; else it returns an error code in p_error.h.
 */
int
p_valid_file(char *path);


/**
 * Parses the domain name and resource path from a URL. This function assumes 
 * the URL to be in the format "http://domain_name/path".
 * @param url - The string with the URL to parse.
 * @param req - Request object to store the information in.
 * @return int - Returns 0 for failure, nonzero for success.
 */
int
p_parse_url(char *url, struct p_http_req *req);


/**
 * 	Used to parse out the header section of an HTTP response, without copying
 * the data, or modifying the original response.
 * @param response - The HTTP response to parse.
 * @param dest - Where to store the header string.
 * @return int - Returns 1 on success; 0 otherwise.
 */
int 
p_parse_http_header(char *response, char *dest);


/**
 * 	This function parses an HTTP GET request, storing the URL path and file
 * strings in the function arguments.
 * The URL must begin with a leading "/", but this character will not be copied
 * for the path.
 * Likewise, in "/file.extension", the leading "/" character is not stored as
 * part of the file string.
 * @param req - The HTTP GET request string.
 * @param path - Where the URL path will be stored.
 * @param file - Where the URL file string will be stored.
 * @return int - Returns 0 on failure, 1 on success.
 */
int 
p_parse_get_req(char *req, char *path, char *file);


/**
 * Given a struct p_http_req object, this function uses the object's path data
 * member to construct an HTTP GET request, which is the stored in the object's
 * req data member.
 * @param req - The object to work with.
 * @return int - Returns 0 on failure, 1 on success.
 */
int
p_make_get_req(struct p_http_req *req);


/**
 * Creates an HTTP GET response header using the constants defined in p_http.h
 * as status codes to determine what sort of header to create.
 * @param file - Used to determine the content-type, if the status code is 
 * 				HTTP_OK.
 * @param header - Where the constructed header is to be stored.
 * @param code - The HTTP status code (defined in p_http.h) of the header.
 * @return void - Nothing is returned.
 */
void
p_make_get_header(char *file, char *header, int code);


/**
 * Used to find where the data section starts (after the header) in an HTTP 
 * response.
 * @param response - The response to search for the data section.
 * @return char * - Returns a pointer to the data section, or NULL on failure.
 */
char *
p_find_data_section(char *response);

#endif











