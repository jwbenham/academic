/* *****************************************************************************
 * File: p_http.c
 * ****************************************************************************/
/** p_http.c
 * 
 * 	Implementation file for all HTTP related functions, structures, etc.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Dec. 05, 2011
 */ 

#include "p_http.h"
#include <stdio.h>

const char *EXT[] = {	".html",
						".htm",
						".HTM",
						".HTML",
						".txt",
						".gif",
						".GIF",
						".jpg",
						".jpeg",
						".JPEG",
						".JPG"	};


int
p_valid_file(char *path)
{
	// Find the last occurrence of "." in the path
	char *dot_ptr = NULL;
	dot_ptr = strrchr(path, '.');
	
	if (dot_ptr == NULL) {
		return P_EBADEXT;
	}
	
	
	// Check if the extension matches an element in EXT
	int i;
	int arr_len = sizeof(EXT)/sizeof(char *);
	
	for (i = 0; i < arr_len; i++) {
		if (strcmp(dot_ptr, EXT[i]) == 0) {
			return 0;
		}
	}
	
	// Otherwise, return error.
	return P_EINVALEXT;	
}


// This function assumes the URL string has already been checked for correctness.
// Also assumes that the url is in the form "http://domain_name/path.extension". 
int
p_parse_url(char *url, struct p_http_req *req)
{
	if (url == NULL) {
		return 0;
	}
	
	// Parse the domain name.
	char *p = url + 7;
	int dom_len = strcspn(p, "/");	// Domain is the string up to first "/".
	strncpy(req->host, p, dom_len);
	req->host[dom_len] = '\0';

	
	// Parse the resource path.
	p += dom_len;	// Move ahead to the beginning "/" of the path.
	strcpy(req->path, p);
	
	// Parse the file name.
	p = strrchr(url, '/') + 1;
	strcpy(req->file, p);
	
	return 1;
}


/* Parses the header section of an HTTP response. It does this by looking for 
 * the end of header markers (CR LF CR LF), replacing the first CR character
 * with ASCII NUL (\0) and copying everything before that point.
 */
int 
p_parse_http_header(char *response, char *dest) 
{
	if ((response == NULL) || (dest == NULL)) {
		return 0;
	}
	
	char eoh[] = {13, 10, 13, 10, 0};	// End Of Header: two CR LF pairs.
	
	char *p = strstr(response, eoh);	// Find the first CR.
	
	if (p == NULL) {	// If it was not found, not a valid header.
		return 0;
	} else {
		*p = '\0';	// Set the first CR to NUL so we can copy the header content,
					// without copying the data section.
		strcpy(dest, response);
		*p = 13;		
	}
	
	return 1;
}


/* Parses a HTTP GET request, storing the requested URL path and file strings
 * in the appropriate function arguments passed in.
 * Returns 0 on failure, 1 otherwise. 
 */
int 
p_parse_get_req(char *req, char *path, char *file) 
{
	
	if (req == NULL) {
		return 0;
	}
	
	// Check that "GET" is in the request.
	char *p = strstr(req, "GET");
	if (p == NULL) {
		return 0;
	} else {
		p += 4;
	}
	
	// Find HTTP/ which should signal the end of the URL.
	char *h = strstr(req, "HTTP/");
	if (h == NULL) {
		return 0;
	}
	
	// Check that the path starts with a "/".
	if (*p != '/') {
		return 0;
	}
	
	// Temporarily terminate the URL with \0.
	h--;
	char tmp = *h;
	*h = '\0';
		
	// Copy the whole path; move p ahead to avoid the first "/".
	strcpy(path, ++p);		

	
	// Copy the file.
	p = strrchr(req, '/');	
	if (p == NULL) {
		return 0;
	}

	strcpy(file, ++p);	// Do not copy the "/" character.
	*h = tmp;
	
	return 1;	
}


/* Makes an HTTP GET request by appending the GET syntax to the struct p_http_req
 * object's path member, and storing the result in req->req.
 */
int
p_make_get_req(struct p_http_req *req) 
{
	if (req->path == NULL) {
		return 0;
	}
	
	// Clear the request char array.
	memset(req->req, '\0', sizeof(req->req));
	
	strcpy(req->req, "GET ");
	strcat(req->req, req->path);
	strcat(req->req, " HTTP/1.0\n\n");

	return 1;
}


void
p_make_get_header(char *file, char *header, int code)
{
	char eoh[] = {13, 10, 13, 10, 0};	// End Of Header: two CR LF pairs.
	
	if (code == HTTP_OK) {
		
		strcpy(header, "HTTP/1.0 200 OK\n");
		strcat(header, "Server: CS465 server\n");
		strcat(header, "Content-type: ");
	
		char *dot = strrchr(file, '.');
	
		int i;
		int len = sizeof(EXT)/sizeof(char *);
		for (i = 0; (i < len) && (strcmp(dot, EXT[i]) != 0); i++) {}
	
		if (i < 4) {	
			strcat(header, "text/html");
		} else if (i == 4) {
			strcat(header, "text/plain");
		} else if ((i > 4) && (i < 7)) {	
			strcat(header, "image/gif");
		} else if (i >= 7) {	
			strcat(header, "image/jpeg");
		}		
		
	}
	else if (code == HTTP_NOT_FOUND) {
		
		strcpy(header, "HTTP/1.0 404 File Not Found");
		
	} else if (code == HTTP_UNSUPPORTED_MEDIA_TYPE) {	
		
		strcpy(header, "HTTP/1.0 415 Unsupported Media Type");
		
	} else if (code == HTTP_BAD_REQUEST) {
		
		strcpy(header, "HTTP/1.0 400 Bad Request");
	
	}
	
	strcat(header, eoh);
}


char *
p_find_data_section(char *response) 
{
	if (response == NULL) {
		return NULL;
	}
	
	char eoh[] = {13, 10, 13, 10, 0};	// End Of Header: two CR LF pairs.
	
	char *p = strstr(response, eoh);
	
	if (p == NULL) {
		return NULL;
	} else {
		return p + 4;
	}	
}


