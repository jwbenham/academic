/**
 * @file 	jb_common.cc
 * @author	Joram Benham <x2008kui@stfx.ca>
 * @version Feb. 17, 2012
 * 
 * @section DESCRIPTION
 * 
 * This is the implementation file for variables and functions common to
 * both the averaging filter and the thresholding programs.
 */

#include "jb_common.h"

namespace JB {

extern const int MASTER = 0;	// Defines the MPI rank of the master 
								// process to make code more readable.
int g_numprocs;	// The number of processes active in MPI_COMM_WORLD is
				// stored here. Prefix g_ for global.

/**
 * get_workspace determines how many pixels a process works on, and
 * what the offset into the array of pixels is for the process.
 * 
 * @param id The MPI rank of the process.
 * @param numpix The number of pixels in the input image.
 * @param alloc Reference to where the function should store the number
 * 			of pixels to be worked on by this process.
 * @param offset Reference to where the function should store the offset
 * 			into the array of pixels at which a process should start.
 * @return void Nothing is returned; results are stored in the 'alloc'
 * 			and 'offset' parameters.
 */
void
get_workspace(const int &id, const int &numpix, int &alloc, int &offset) 
{
	int quotient;
	int remainder;
	
	quotient = numpix / g_numprocs;
	remainder = numpix % g_numprocs;
	
	alloc = quotient;		// Base allocation for all procs.
	offset = id * quotient;	// Initial offset for all procs.
	
	// Procs with id < remainder get an extra pixel. Their offset is
	// increased by 1 for every other proc with an extra pixel which
	// precedes them.
	if (id < remainder) {
		alloc++;	
		offset += id;
	}
	
	// Procs without an extra pixel have their offset increased by the
	// number of remainder pixels.
	else {
		offset += remainder;
	}
}


}
