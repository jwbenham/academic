#academic

This repository holds the source code for a selection of academic projects. 
Information on each project is available in the README.md file included in 
each project's directory.  

The projects typically need to be built and run from a command line. Java
projects should work in any environment with an installed Java virtual machine.
C/C++ projects are typically written with Linux-specific libraries, and therefore
will only run under Linux (or, possibly other POSIX-compliant operating systems).

In the following list of projects, each list item begins with the project name,
followed by the language(s) used to develop the project, and the operating
system(s) the project runs on.

* __guestbook-client-server [Java - Windows/Unix]__: A multithreaded guestbook 
client/server 
project.
* __chat-client-server [C - Linux/Mac OS X]__: A multithreaded HTTP chat server, 
and an HTTP client which will connect to either 1) an HTTP server to submit HTTP
requests, or 2) the accompanying HTTP chat server for chatting.
* __http-client-server [C - Linux/Mac OS X]__: A (limited) multithreaded HTTP web 
server, and a (limited) HTTP web client. The HTTP client can request and download 
files from a web server. Likewise, the HTTP server can serve files to a HTTP 
client (the one included in this project, or a more advanced one like a web browser).
* __parallel-image-filters [C++ - Linux]__: A project containing two image filters
written using parallel programming algorithms. The OpenMPI library (or any MPI
implementation) is required to build and run these filters.

