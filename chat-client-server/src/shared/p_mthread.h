/* *****************************************************************************
 * File: p_mthread.h
 * ****************************************************************************/
/** p_mthread.h
 *	 
 * 	Very basic file. Basically just includes the POSIX threads library and
 * defines two macros to shorten mutex function calls.
 * 
 * @author Joram Benham <x2008kui@stfx.ca>
 * @date Nov. 15, 2011
 * 
 */

#ifndef _P_MTHREAD_H_
#define _P_MTHREAD_H_

#include <pthread.h>


#define MXLOCK(MX) pthread_mutex_lock(MX)
#define MXUNLOCK(MX) pthread_mutex_unlock(MX)


#endif

