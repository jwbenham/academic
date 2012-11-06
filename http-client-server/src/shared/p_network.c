/* *****************************************************************************
 * File: p_network.c
 * ****************************************************************************/
/** p_network.c
 *	Contains the implementation of network related functions. These are used
 * both by the client and the server.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Nov. 15, 2011
 */ 

#include "p_network.h"


/* *****************************************************************************
 * Public function definitions
 * ****************************************************************************/
 
void p_init_conn_params(struct p_conn_params *cp) 
{
	cp->server = NULL;
	cp->port = NULL;
	
	cp->open = NULL;
	cp->socket = -1;
	cp->active = 0;
} 
 
void 
p_init_addrinfo(struct addrinfo *ai, int flags, int fam, int sock, int prot) 
{
	memset(ai, 0, sizeof(ai));
	
	if (flags >= 0) {
		ai->ai_flags = flags;
	}
	if (fam >= 0) {
		ai->ai_family = fam;
	}
	if (sock >= 0) {
		ai->ai_socktype = sock;
	}
	if (prot >= 0) {
		ai->ai_protocol = prot;
	} 
}

int 
p_find_socket(struct p_conn_params *cp) 
{
	struct addrinfo *it = cp->server_info;
	cp->socket = -1;
	
	for (; it != NULL && cp->socket < 0; it = it->ai_next) {
		cp->socket = socket(it->ai_family, it->ai_socktype, it->ai_protocol);

		if (cp->socket >= 0) {
			cp->open = it;
		}
	}
	
	if (cp->socket < 0) {
		return 0;
	}	
	
	return 1;
}


int
p_close_socket(int *sock) 
{
	if (close(*sock) < 0) {
		return p_perror(P_ECSOCK, strerror(errno));
	}
	
	return 0;
}


int 
p_connect(struct p_conn_params *cp) 
{	
	int status = getaddrinfo(cp->server, cp->port, &cp->params, &cp->server_info);		
	if (status != 0) {	// Lookup failed.
		return p_perror(P_EGAI, gai_strerror(status));
	}
	
	
	if (p_find_socket(cp) == 0) { // Could not open socket.
		freeaddrinfo(cp->server_info);
		return p_perror(P_EOSOCK, "In p_connect(), after call to p_find_socket()");
	}
			
			
	cp->active = connect(cp->socket, cp->open->ai_addr, cp->open->ai_addrlen);

	if (cp->active < 0) {	// Call to connect() failed.
		freeaddrinfo(cp->server_info);
		return p_perror(P_ECONN, strerror(errno));
	}		
	
					
	freeaddrinfo(cp->server_info);	// No longer needed.
	cp->active = 1;	// If no error, connection is open/active.
	
	return 0;	
}


int
p_send_msg(int *sock, char *msg, int sz) 
{
	int retval;
	int sent = 0;	// Bytes sent so far.
	int rem = sz - sent;	// Bytes remaining to send.
	
	while ((rem > 0) && (retval != -1)) {
		retval = send(*sock, (void *)msg, rem, 0);
		
		if (retval != -1) {
			sent += retval;
			rem = sz - sent;
		}
	}
	
	return (retval != -1);	// Returns 0 on error.
}


int
p_test_conn(int sock)
{
	char buf[1];
	int status = recv(sock, buf, 1, MSG_DONTWAIT | MSG_PEEK);
	
	if ((status == EAGAIN) || (status == EWOULDBLOCK) || (status > 0)) {
		return 1;
	}
	
	return status;	// 0 - closed; < 0 - error.
}


// Returns less than 0 on error, 0 for too little space, 1 for overall success.
int
p_get_sock_name(int sockfd, struct sockaddr *sa, unsigned sa_len)
{
	int old_sa_len = sa_len;
	int ret = getsockname(sockfd, sa, &sa_len);
	
	if (ret < 0) {
		p_perror(P_EGERR, strerror(errno));
		return ret;
	}
	
	if (ret == 0) {
		if (old_sa_len < sa_len) {
			return 0;
		}
		else {
			return sa_len;
		}
	}	
}


int
p_port_from_sa(struct sockaddr *sa) 
{
	if (sa->sa_family == AF_INET) {
		return (int)ntohs(((struct sockaddr_in*)sa)->sin_port);
	}
	
	else if (sa->sa_family == AF_INET6) {
		return (int)ntohs(((struct sockaddr_in6*)sa)->sin6_port);
	}
	
	return -1;
}


char *
p_ip_from_sa(struct sockaddr *sa, char *dst) 
{
	if (sa->sa_family == AF_INET) {
		return (char *)inet_ntop(AF_INET, &((struct sockaddr_in *)sa)->sin_addr, dst,
								 INET_ADDRSTRLEN);
	}
	
	else if (sa->sa_family == AF_INET6) {
		return (char *)inet_ntop(AF_INET6, &((struct sockaddr_in6 *)sa)->sin6_addr, dst,
								 INET6_ADDRSTRLEN);
	}
	
	return NULL;
}


void
p_print_if_addrs(void)
{
	struct ifaddrs *results = NULL;
    struct ifaddrs *ifa = NULL;
    char ipstr[INET6_ADDRSTRLEN];
	int num;
	
    getifaddrs(&results);
	
	printf("-----------------------------------------");
	printf("\nLocal Machine Interface Addresses:");
	if (results == NULL) {
		printf("\n\tNo local interface addresses found.\n");
	}
	
    for (ifa = results, num = 0; ifa != NULL; ifa = ifa->ifa_next) {
		memset(ipstr, '\0', sizeof(char) * INET6_ADDRSTRLEN);
		p_ip_from_sa((struct sockaddr *)ifa->ifa_addr, ipstr);
		
		if (strlen(ipstr) > 2) {
			printf("\n\t(%i)\t%s", num++, ipstr);
			fflush(stdout);
		}
	}

	printf("\n-----------------------------------------\n");
	
	if (results != NULL) {
		freeifaddrs(results);
    }	
}
	




