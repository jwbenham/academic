/**
 * @file 	jb_threshold.cc
 * @author	Joram Benham <x2008kui@stfx.ca>
 * @version Feb. 17, 2012
 * 
 * @section DESCRIPTION
 * 
 * This file contains the implementation of the MPI PPM image
 * thresholding program.
 */
 
#include <iostream>
#include <string>
#include <stdexcept>
#include <cstdlib>
#include <cctype>
#include <cstdio>
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
bool proc_threshold(const int &, const string &, const string &);
double get_partial_intensity(JB::Pix_256 *, int, const int &);
void threshold_workspace(JB::Pix_256 *, int, const double &); 
void check_args(int &, char **);


/**
 * Main accepts user input, checks that it is valid, and starts the
 * main thresholding procedure.
 * 
 * @param argc Number of command line arguments.
 * @param argv Pointer to the command line argument strings.
 * @return int Returns 0 if there are no errors; nonzero otherwise.
 */
int main(int argc, char *argv[]) 
{
	bool threshold_success;
	int myid;
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
	}
	catch (invalid_argument &ia) {
		if (myid == JB::MASTER) {
			cerr << "**Error: " << ia.what();
			cout << "\nUsage: thresh <input> <output>\n";
		}
		MPI_Finalize();		
		return 1;		
	}

	// Start the timer.
	starttime = MPI_Wtime();
	
	// Thresholding procedure.
	threshold_success = proc_threshold(myid, input, output);
	
	// End the timer.
	endtime = MPI_Wtime();
	runtime = endtime - starttime;
	
	if (threshold_success && myid == JB::MASTER) {
			printf("\nthresholding runtime: %10.10f \n", runtime);
	}
	
	// Successful termination
	MPI_Finalize();
	return !threshold_success;
}


/**
 * proc_threshold implements the logic for the image thresholding. All
 * processes execute this function. Code which is specifically for the
 * master process is enclosed in conditional blocks.
 * 
 * @param myid The MPI rank of the process.
 * @param in The path to the input image. Significant only to process 0.
 * @param out The desired name for the output image. Significant only
 * 			to process 0.
 * @return void Nothing is returned.
 */
bool
proc_threshold(const int &myid, const string &in, const string &out)
{
	// MASTER only variables.
	JB::PPM_File in_ppm(in);	// Input file.
	JB::PPM_File out_ppm(out);	// Output file.
	JB::PPM_Reader reader;		// Reads file in.
	JB::PPM_Writer writer;		// Reads file out.
	
	// Arrays with number of elements equal to number of processes.
	int *offsets = NULL;	// Start of each processes workspace in the PPM.
	int *allocs = NULL;		// Number of pixels each proc. is assigned.
	
	// MASTER and SLAVE variables.
	int read_success;
	int numpix, allocation, start;
	double partial_intensity, avg_intensity;
	JB::Pix_256 *data;
	
	
	if (myid == JB::MASTER) {
		// Get the PPM file.
		try {
			reader.m_read_ppm(in_ppm);
			read_success = 1;
			numpix = in_ppm.height * in_ppm.width;
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
	
	// Tell everyone how many pixels there are.
	MPI_Bcast(&numpix, 1, MPI_INT, JB::MASTER, MPI_COMM_WORLD);
	
	// Determine this proc's workspace.
	JB::get_workspace(myid, numpix, allocation, start);	
	
	allocation *= 3;	// Scale allocation and start to the byte
	start *= 3;			// level (3 bytes per pixel.)
	data = new JB::Pix_256 [allocation/3];
	
	// Get the start offsets and allocations for each proc. Note that
	// although there are 'numpix' pixels (type Pix_256), the sending
	// is done treating them as characters. Thus the offsets 
	// and allocations are three times what they would be dealing with
	// just pixels.
	if (myid == JB::MASTER) {
		offsets = new int [JB::g_numprocs];
		allocs = new int [JB::g_numprocs];
	}
	
	MPI_Gather(&start, 1, MPI_INT, offsets, 1, MPI_INT, JB::MASTER, MPI_COMM_WORLD);
	MPI_Gather(&allocation, 1, MPI_INT, allocs, 1, MPI_INT, JB::MASTER, MPI_COMM_WORLD); 
	
	// Send out pixel allocations based on the offsets.
	MPI_Scatterv(in_ppm.pixels, allocs, offsets, MPI_UNSIGNED_CHAR, data, 
				 allocation, MPI_UNSIGNED_CHAR, JB::MASTER, MPI_COMM_WORLD);
	
	// Calculate partial intensity for the workspace.
	partial_intensity = get_partial_intensity(data, allocation/3, numpix);
	
	// Combine partial intensities of all processes, and redistribute.
	MPI_Allreduce(&partial_intensity, &avg_intensity, 1, MPI_DOUBLE, 
				  MPI_SUM, MPI_COMM_WORLD);
	
	// Each proc thresholds its workspace.
	threshold_workspace(data, allocation/3, avg_intensity);
	
	// Gather the modified workspaces.
	if (myid == JB::MASTER) out_ppm.pixels = new JB::Pix_256 [numpix];
	else out_ppm.pixels = NULL;
	
	MPI_Gatherv(data, allocation, MPI_UNSIGNED_CHAR, out_ppm.pixels, allocs,
				offsets, MPI_UNSIGNED_CHAR, JB::MASTER, MPI_COMM_WORLD);
	
	if (myid == JB::MASTER) {
		// Write out the PPM file.
		out_ppm.height = in_ppm.height;
		out_ppm.width = in_ppm.width;
		out_ppm.maxval = in_ppm.maxval;
		writer.m_write_ppm(out_ppm, out_ppm.file_name);
		
		// Clean up
		delete [] allocs;
		delete [] offsets;
	}
	
	return true;
}


/**
 * get_partial_intensity calculates the sum of the pixel intensities 
 * within the workspace of the image assigned to an individual process.
 * It then divides this sum by the total number of pixels in the entire
 * image (not just in the workspace). The partial intensities can then
 * be summed by the master process to arrive at the average intensity 
 * for the entire image.
 * 
 * @param data The array of pixels being worked on by a process.
 * @param sz The size of the data array.
 * @param numpix The number of pixels in the input image.
 * @return double The partial intensity for the array of pixels.
 */
double
get_partial_intensity(JB::Pix_256 *data, int sz, const int &numpix) 
{
	JB::Pix_256 *pix = data;
	double pix_sum = 0;
	double intensity = 0;
		
	for (int i = 0; i < sz; i++, pix++) {
		pix_sum = 1.0 * (double)pix->r 
				+ 1.0 * (double)pix->g
				+ 1.0 * (double)pix->b;
		
		intensity += pix_sum / 3.0;
	}
	
	return intensity/numpix;
}


/**
 * threshold_workspace performs thresholding for a subset of the pixels
 * in the input image, given the image's average intensity. If a pixel
 * in the workspace has higher intensity than the average, it is 
 * coloured white, otherwise it is coloured black.
 * 
 * @param data The array of pixels being worked on by a process.
 * @param sz The size of the data array.
 * @param avg The average intensity of the image.
 * @return void Nothing is returned; the function modifies the elements
 * 			of 'data'.
 */
void
threshold_workspace(JB::Pix_256 *data, int sz, const double &avg) 
{
	JB::Pix_256 *pix = data;
	double pix_intensity;
	unsigned char value;
	
	for (int i = 0; i < sz; i++, pix++) {
		pix_intensity = (1.0 * (double)pix->r 
						 + 1.0 * (double)pix->g
						 + 1.0 * (double)pix->b) / 3.0;
		
		if (pix_intensity > avg) value = 255;
		else value = 0;
		
		pix->r = pix->g = pix->b = value; 
	}
}


/**
 * check_args performs a simple check to ensure the number of command
 * line arguments is correct.
 * 
 * @param argc The number of arguments.
 * @param argv The command line argument strings.
 * @return void Nothing is returned; an exception is thrown if the
 * 		arguments are invalid.
 */
void 
check_args(int &argc, char *argv[]) 
{
	if (argc < 3) throw invalid_argument("too few arguments");
}


