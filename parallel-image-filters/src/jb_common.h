/**
 * @file 	jb_common.h
 * @author	Joram Benham <x2008kui@stfx.ca>
 * @version Feb. 17, 2012
 * 
 * @section DESCRIPTION
 * 
 * This is the specification file for variables and functions common to
 * both the averaging filter and the thresholding programs.
 * Documentation is in jb_common.cc.
 */

#ifndef JB_COMMON_H
#define JB_COMMON_H

namespace JB {

extern const int MASTER;	
extern int g_numprocs;

void 
get_workspace(const int &, const int &, int &, int &);

}

#endif
