# Makefile for CSCI-465 client program.
# Author:		Joram Benham <x2008kui@stfx.ca>
# Date:			Nov. 15, 2011

CLIENT = cli
SERVER = serv
CC = gcc

CFLAGS = -pthread -g -I ./src
CLI_DIR = ./src/client
SERV_DIR = ./src/server
SHARE_DIR = ./src/shared

C_OBJS = $(CLI_DIR)/client.o 
S_OBJS = $(SERV_DIR)/server.o
S_OBJS += $(SERV_DIR)/handler.o

SHARE_OBJS = $(SHARE_DIR)/p_error.o 
SHARE_OBJS += $(SHARE_DIR)/p_network.o


all:	client server

client:	$(C_OBJS) $(SHARE_OBJS)
	$(CC) -o $(CLIENT) $(CFLAGS) $(C_OBJS) $(SHARE_OBJS)

server:	$(S_OBJS) $(SHARE_OBJS)
	$(CC) -o $(SERVER) $(CFLAGS) $(S_OBJS) $(SHARE_OBJS)
	
%.o:	%.c
	$(CC) -o $*.o $(CFLAGS) -c $*.c	
	
clean:
	rm -f -r $(CLIENT) $(SERVER) $(C_OBJS) $(S_OBJS) $(SHARE_OBJS) 
