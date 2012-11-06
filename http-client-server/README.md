#http-client-server
This is an HTTP client/server project written in C for a networking course. It is
a continuation/modification of the original chat-client-server project. It
essentially provides a very simple/limited HTTP client program (httpget) and
server program (httpserv).
* __httpget__: Allows the user to request and download resources (html pages, 
text files, .gif and .jpeg images) from an HTTP web server.
* __httpserv__: Allows the user to run a very limited HTTP server from their
own computer. It is limited to serving the same resources as the client is
limited to requesting. However, it is robust enough that it should properly
serve full web pages to a modern browser.

__Requirements__
* Linux/Mac OS X (possibly other POSIX compliant OS)

## Using the HTTP Client (httpget)
To build the httpget program navigate to the Makefile directory in a terminal,
and execute `make client`.  

The httpget program can be run from its directory by executing the following:  
`./httpget <url> <port>`  
* __url__ is the URL of the resource to download (an image, document, etc).
* __port__ is the port number to try to connect on.  

__Example 1__: Downloading the StFX logo from sites.stfx.ca  
`./httpget http://sites.stfx.ca/assets/images/stfx_logo.jpg 80`

__Note__: The following URL resource extensions are supported by the client:
.html, .htm, .HTML, .HTM, .txt, .gif, .jpg, .jpeg.

## Using the HTTP Server (httpserv)
To build the httpserv program navigate to the Makefile directory in a terminal,
and execute `make server`.  

To run the HTTP server, enter `./httpserv [port]` at the terminal prompt. 

The __port__ parameter is optional. If left unspecified, the server will default to
listening on port 80. Note that in order to bind to port 80, you might have to
execute the program with super-user rights (e.g. with the `sudo` command).
Otherwise, run the server on a port which is not a well-known or protected port.

