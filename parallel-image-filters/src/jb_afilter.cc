/**
 * @file 	jb_afilter.cc
 * @author 	Joram Benham <x2008kui@stfx.ca>
 * @version Feb. 17, 2012
 * 
 * @section DESCRIPTION
 * 
 * 	This file contains the implementation of the parallel image 
 * averaging filter program. 
 */
 
#include <iostream>
#include <string>
#include <stdexcept>
#include <cstdlib>
#include <cstdio>
#include <cctype>
#include <mpi.h>

#include "jb_common.h"
#include "jb_ppm.h"

using std::cout;
using std::cerr;
using std::cin;
using std::endl;
using std::string;
using std::invalid_argument;


/* Function Declarations */
bool proc_afilter(const int &, const string &, const string &, 
				  const int &, const int &);
void avg_filter(JB::Pix_256 *, const int &, JB::Pix_256 *, const int &, 
		        const int &, const int &, const int &, const int &, 
		        const int &);
void check_args(int &, char **);
bool is_num(string);


/**
 * Main accepts user input, checks that it is valid, and starts the
 * image averaging filter procedure.
 * 
 * @param argc Number of command line arguments.
 * @param argv Pointer to the command line argument strings.
 * @return int Returns 0 if there are no errors; nonzero otherwise.
 */
int main(int argc, char *argv[]) 
{
	bool filter_success;
	int myid, xrad, yrad;
	string input, output;
	double starttime, endtime, runtime;
	
	// Initialize MPI; get number of procs; get this proc's rank.
	MPI_Init(&argc, &argv);
	MPI_Comm_size(MPI_COMM_WORLD, &JB::g_numprocs);
	MPI_Comm_rank(MPI_COMM_WORLD, &myid);
	
	// Check arguments
	try {
		check_args(argc, argv);
		input = argv[1];
		output = argv[2];
		xrad = std::abs(std::atoi(argv[3]));
		yrad = std::abs(std::atoi(argv[4]));
	}
	catch (invalid_argument &ia) {
		if (myid == JB::MASTER) {
			cerr << "**Error: " << ia.what();
			cout << "\nUsage: afilter <input> <output> <x radius> <y radius>\n";
		}
		MPI_Finalize();		
		return 1;		
	}

	// Start the timer.
	starttime = MPI_Wtime();

	// Averaging filter procedure.
	filter_success = proc_afilter(myid, input, output, xrad, yrad);
	
	// End the timer.
	endtime = MPI_Wtime();
	runtime = endtime - starttime;
	
	if (filter_success && myid == JB::MASTER) {
		printf("\naveraging filter runtime: %10.10f \n", runtime);	
	}

	// Successful termination
	MPI_Finalize();	
	return !filter_success;
}


/**
 * proc_afilter implements the communication and data processing for the
 * averaging filter. Both the master and slave processes execute this
 * procedure. Code which is specific to the master process is enclosed
 * in conditional blocks. 
 * 	Note that ws_ stands for "workspace", which refers to the pixels
 * allocated to each process for filtering. That is, the workspace for
 * process is the section of the PPM image which the process will be 
 * turning "fuzzy". In order to do this, each process needs information
 * about the pixels surrounding its workspace. The number and extent of
 * these extra pixels is determined by the x and y radius of the 
 * rectangle. A process is allocated, in addition to its workspace, 
 * an area several rows above and below its first and last workspace
 * pixels. With this extra allocation, the process has all the pixel
 * information it needs to do the filtering. This extra allocation is
 * prefixed with "rec_".
 * 
 * @param myid The executing process's MPI rank.
 * @param in The file path of the input PPM.
 * @param out The desired file path of the output PPM.
 * @param xrad The x radius of the rectangle.
 * @param yrad The y radius of the rectangle.
 * @return void Nothing is returned.
 */
bool
proc_afilter(const int &myid, const string &in, const string &out,
			const int &xrad, const int &yrad)
{
	// MASTER only variables.
	JB::PPM_File in_ppm(in);
	JB::PPM_File out_ppm(out);	
	JB::PPM_Reader reader;
	JB::PPM_Writer writer;
	
	// Arrays, each with number of elements equal to number of processes.
	int *ws_allocs;	// Array of number of pixels filtered by each proc.
	int *ws_offsets;	// Where each proc.'s workspace starts.
	int *area_offsets;	// The beginning of the extra rows sent to each
						// proc. so they can do their averaging.
	int *area_allocs;	// The number of pixels in the extra allocations.
	
	ws_allocs = ws_offsets = area_offsets = area_allocs = NULL;		
	
	// MASTER-SLAVE variables.
	int read_success;
	int numpix;
	int ws_alloc, ws_offset, area_alloc;
	int ws_startrow, ws_endrow, area_startrow, area_endrow;
	int inner_start, inner_end;
	JB::Pix_256 *area, *filtered_ws;
	
	
	// Read in the PPM file, allocate space for arrays.
	if (myid == JB::MASTER) {
		try {
			reader.m_read_ppm(in_ppm);
			read_success = 1;
			
			out_ppm.width = in_ppm.width;
			out_ppm.height = in_ppm.height;
			out_ppm.maxval = in_ppm.maxval;
			out_ppm.pixels = new JB::Pix_256[out_ppm.width * out_ppm.height];
		
			ws_allocs = new int [JB::g_numprocs];
			ws_offsets = new int [JB::g_numprocs];
			area_offsets = new int [JB::g_numprocs];
			area_allocs = new int [JB::g_numprocs];
		}
		catch (std::exception &e) {
			cerr << "**Error: " << e.what();
			cout << endl << "Failed to read file; terminating.\n";
			read_success = 0;
		}
	}
	
	// Broadcast whether or not to continue.
	MPI_Bcast(&read_success, 1, MPI_INT, JB::MASTER, MPI_COMM_WORLD);
	if (read_success == 0) return false;
	
	// Broadcast the number of pixels.
	MPI_Bcast(&out_ppm.width, 1, MPI_INT, JB::MASTER, MPI_COMM_WORLD);
	MPI_Bcast(&out_ppm.height, 1, MPI_INT, JB::MASTER, MPI_COMM_WORLD);
	numpix = out_ppm.width * out_ppm.height;
	
	// Determine this process's workspace; send it to the master.
	JB::get_workspace(myid, numpix, ws_alloc, ws_offset);

	/* In this next section, we determine how many rows of the PPM
	 * a process requires to perform its filter. This is the working
	 * area. For example, if a process is to filter the pixels in 
	 * columns 100 to 200 on row 5, and the rectangle y-radius is 2, 
	 * then the process has to have access to the other pixels from row 
	 * 3 to row 7 - two rows away from row 5. 
	 */
	// Get the start and end rows of this process's workspace.
	ws_startrow = ws_offset / out_ppm.width;	
	ws_endrow = (ws_offset + (ws_alloc - 1)) / out_ppm.width;
	
	// Get the starting row of the working area.
	area_startrow = (ws_startrow - yrad) * out_ppm.width;		
	if (area_startrow < 0) area_startrow = 0;			
	
	// Get the ending row of the working area.
	area_endrow = (ws_endrow + yrad) * out_ppm.width;	
	if (area_endrow >= out_ppm.height * out_ppm.width) {
		area_endrow = (out_ppm.height - 1) * out_ppm.width;
	}
	
	// Find the allocation, given the start/end of the working area.
	area_alloc = area_endrow - area_startrow + out_ppm.width;

	// Scale allocations and offsets to byte level (3 bytes per pixel).
	ws_offset *= 3;	
	ws_alloc *= 3;
	area_startrow *= 3;
	area_alloc *= 3;

	// Gather offsets/allocations later for image reconstruction.
	MPI_Gather(&ws_offset, 1, MPI_INT, ws_offsets, 1, MPI_INT, 
			   JB::MASTER, MPI_COMM_WORLD);
	MPI_Gather(&ws_alloc, 1, MPI_INT, ws_allocs, 1, MPI_INT, JB::MASTER,
			   MPI_COMM_WORLD);

	// Gather the pixels required by each process.
	MPI_Gather(&area_startrow, 1, MPI_INT, area_offsets, 1, MPI_INT, 
			   JB::MASTER, MPI_COMM_WORLD);
	MPI_Gather(&area_alloc, 1, MPI_INT, area_allocs, 1, MPI_INT, 
			   JB::MASTER, MPI_COMM_WORLD);

	// Send out the pixels to the processes.
	area = new JB::Pix_256[area_alloc/3];
	filtered_ws = new JB::Pix_256[ws_alloc/3];
	
	MPI_Scatterv(in_ppm.pixels, area_allocs, area_offsets, MPI_UNSIGNED_CHAR,
				 area, area_alloc, MPI_UNSIGNED_CHAR, JB::MASTER, MPI_COMM_WORLD);	
	
	// Find the offset within the working area, scaled to pixels, not bytes.
	inner_start = (ws_offset - area_startrow)/3;
	inner_end = inner_start + ws_alloc/3;

	// Run the averaging filter algorithm.
	avg_filter(area, area_alloc/3, filtered_ws, inner_start, inner_end,
			   out_ppm.width, out_ppm.height, xrad, yrad);
	
	MPI_Gatherv(filtered_ws, ws_alloc, MPI_UNSIGNED_CHAR, out_ppm.pixels, ws_allocs,
				ws_offsets, MPI_UNSIGNED_CHAR, JB::MASTER, MPI_COMM_WORLD);
	
	// Write out the image and deallocate memory.
	if (myid == JB::MASTER) {
		writer.m_write_ppm(out_ppm, out_ppm.file_name);
		
		delete [] ws_allocs;
		delete [] ws_offsets;
		delete [] area_offsets;
		delete [] area_allocs;
		delete [] area;
		delete [] filtered_ws;
	}
	
	return true;
}


/**
 * avg_filter, given an 'area' of an image to work in, and a start and
 * end of the workspace within the 'area', applies the averaging
 * filter procedure to the workspace and returns the resulting filtered
 * pixels in the 'result' parameter. Note that the 'area' array of 
 * Pix_256 pixels should contain enough entries to service the workspace
 * defined by 'start' and 'end', given the rectangle x radius (xrad)
 * and y radius (yrad). The filter for each pixel 'p' in the workspace is
 * determined by using the RGB values of the pixels in the rectangle
 * surrounding 'p'.
 * 
 * @param area An array of pixels (Pix_256) containing (1) the pixels
 * 			to apply the filter to (called the workspace) and (2) a 
 * 			superset of (1) which supplies the information about the
 * 			pixels surrounding each pixel in the workspace.
 * @param area_sz The number of elements in the area.
 * @param result An array of size 'start' - 'end', which will hold the
 * 			pixels which result from the filtering process.
 * @param start The index of the starting element of the workspace 
 * 			within the 'area'.
 * @param end The index of the ending element of the workspace (not 
 * 			included in the workspace).
 * @param w The width of the PPM image.
 * @param h The height of the PPM image.
 * @param xrad The x radius of the rectangle.
 * @param yrad The y radius of the rectangle.
 */
void
avg_filter(JB::Pix_256 *area, const int &area_sz, JB::Pix_256 *result,
		   const int &start, const int &end, const int &w, const int &h,
		   const int &xrad, const int &yrad) 
{
	int p, pix;
	int num, r, g, b;
	int x, y, xas, yas;
	
	for (p = start; p < end; p++) {
		y = p / w;
		x = p % w;
		num = r = g = b = 0;
		
		// Get the average of the rectangle around pixel "p".
		for (xas = x - xrad; xas < x + xrad; xas++) {
			 for (yas = y - yrad; yas < y + yrad; yas++) {
				pix = (yas * w) + xas;
				
				if (pix < 0 || pix >= area_sz) continue;
				
				else {
					r += area[pix].r;
					g += area[pix].g;
					b += area[pix].b;
					num++;
				}
			}
		} 
		
		// Create the resulting average filtered pixel.
		if (num > 0) {
			result[p-start].r = r/num;
			result[p-start].g = g/num;
			result[p-start].b = b/num;
		}
	}	
}


/** 
 * check_args does some basic validation of the command line arguments
 * and throws an exception if they are invalid.
 * 
 * @param argc The number of arguments provided by the user.
 * @param argv Array of strings representing the command line arguments.
 * @return void Nothing is returned; an exception is thrown if an 
 * 			argument is invalid. 
 */
void 
check_args(int &argc, char *argv[]) 
{
	if (argc < 5) throw invalid_argument("too few arguments");
	if (!is_num(argv[3]) || !is_num(argv[4])) {
		throw invalid_argument("x-radius and y-radius must be integers");
	}
}


/**
 * is_num checks if a string represents a decimal integer.
 * @param str The string to check.
 * @return bool Returns true if the string represents a decimal integer.
 */
bool
is_num(string str) 
{
	for (int i = 0; i < str.length(); i++) {
		if (std::isdigit(str[i]) == false) return false;
	}
	
	return true;
}

