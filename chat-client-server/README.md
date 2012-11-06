#chat-client-server
This directory holds the source code for an HTTP chat client/server developed 
for a networking course. The client and server are developed in C. Multithreading
is implemented using the POSIX Threads (Pthreads) API. Networking is implemented
using Unix libraries (i.e. netdb.h, ifaddrs.h, sys/socket.h). Consequently, the
programs work only on Linux/Mac OS X, and not Windows. The following 
functionality is provided:
* _HTTP Client_: A command-line HTTP client. Given the domain name or the IP 
address of an HTTP server, along with a port number, the client will connect
to the server and allow the user to send HTTP messages and receive server
responses.
    * If the client connects to a regular HTTP server (e.g. www.google.ca),
	messages must be sent following the HTTP protocol.
    * If the client connects to the HTTP chat server included in this directory,
	then the user may send ordinary text messages to the server and all other
	client connected to the chat server.
* _HTTP Server_: A command-line HTTP chat server. This is a chat server designed
specifically to work with the accompanying HTTP chat client. Clients connected
to the server will have all of their sent messages sent to all other connected
clients, and will receive all messages sent by other clients.

## Using the Server
The HTTP chat server can be compiled using the supplied Makefile, by entering
`make server`.  

The chat server can be run with the following command (from the directory the
server is located in):  
`./serv [port]`  
[port] is an optional parameter the user can supply to specify what port the
server should listen on.

## Using the Client
The HTTP client can be compiled by moving to the top-level project directory and
entering `make client` in a terminal.  

From a terminal, the client can be run with the command  
`./cli <hostname/ip> <port>`  
where __hostname/ip__ specifies the IP address or the domain name of the HTTP
server to connect to. The __port__ parameter specifies the port the client
should try to connect on.  
  
__Example 1__: Connecting to a regular HTTP server with the client.  
`./cli www.google.ca 80` -> try to connect to the server listening on port 80
at www.google.ca.  
  
__Example 2__: Connecting to the HTTP chat server.  
`./cli localhost 19000` -> try to connect to the HTTP chat server running on the 
local machine, listening on port 19000.
