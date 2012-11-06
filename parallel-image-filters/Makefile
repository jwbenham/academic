# File: 	Makefile
# Author:	Joram Benham <x2008kui@stfx.ca>
# Date:		Feb. 17, 2012
#	This is the Makefile for the CS-455 Project 1.
#
# - Use "make", "make afilter", or "make threshold" to build the 
# programs using OpenMPI.
# - Use "make sun", "make sun_afilter", or "make sun_threshold" to 
# build the programs on the SUN cluster (SUN MPI).
#

COMMON=src/jb_ppm.cc src/jb_common.cc

all: afilter threshold

afilter:
	mpiCC src/jb_afilter.cc $(COMMON) -o afilter

threshold:
	mpiCC src/jb_threshold.cc $(COMMON) -o threshold
	
sun: sun_afilter sun_threshold

sun_afilter:
	mpCC src/jb_afilter.cc $(COMMON) -o afilter -lmpi
	rm *.o
	
sun_threshold:
	mpCC src/jb_threshold.cc $(COMMON) -o threshold -lmpi
	rm *.o
	
clean:
	rm -f afilter threshold
