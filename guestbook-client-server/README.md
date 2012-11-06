#Guestbook-Client-Server

This Java application provides a multithreaded guestbook server backed by an 
Oracle database, and a guestbook client which allows users to connect to the 
server. Users connected through the guestbook client can leave a message, which
the server will store in the database.

##Purpose
This is an academic project. It was developed as an end-of-term assignment to
demonstrate knowledge of Java Swing, JDBC, networking, and multithreading.

##Using on Linux
The application can be built using the provided Makefile in a terminal:  
`make all`  
  
Two shell scripts are provided to simplify running the client/server. The server
can be started by running `./run-server.sh`. Likewise, the client can be started
by running `./run-client.sh`.

##Using on Windows
The application can be built on Windows by running the following command in
the Windows command line:  
`javac -cp ./src -Xlint:unchecked @src-files.txt`  

In a command line window, the server can be executed with the command
`java -cp ./src:./lib/* guestbook.server.ServerGUI`. Similarly, the client can
be executed with the command `java -cp ./src guestbook.client.ClientGUI`.

##Running the Server
When the server is started, a screen will appear which provides configuration
options and buttons to start or stop the server. The configuration options
are as follows:
* _Port #_: Specifies the port number for the server to listen on.
* _Client Handlers_: Specifies the maximum number of clients allowed to be
connected at any one time.
* _Timeout Interval(ms)_: The server will wait this long for a client collection
before checking to see if the user has commanded it to stop.
* _Database URL_: The URL of the Oracle database server.
* _Database Username_: Username to log onto the database server with.
* _Database Password_: User password to log onto the database server with.

##Running the Client
When it is first started, the client displays a screen for the user to connect 
to a guestbook server and enter login information. The user can also choose to
register a new account, once connected to the server. The connection options are:
* _Server Host (name or IP)_: The domain name or IP address of the computer
hosting the guestbook server to connect to.
* _Server Port_ The port the guestbook server is listening on.
  
The login fields are:
* _Email_: A valid user email registered with the guestbook server.
* _Password_: The password associated with the submitted email address.

