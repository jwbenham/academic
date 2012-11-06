/**
 * @file 	jb_ppm.h
 * @author	Joram Benham <x2008kui@stfx.ca>
 * @version Feb. 17, 2012
 * 
 * @section DESCRIPTION
 * 
 * This is the specification file for classes dealing with PPM image
 * files. They are defined within the JB namespace. Class methods are
 * prefixed with 'm_'.
 */

#ifndef JB_PPM_H
#define JB_PPM_H

#include <string>
#include <fstream>
#include <cstdlib>
#include <cctype>

using std::string;
using std::ios;
using std::ios_base;
using std::ifstream;
using std::ofstream;
using std::skipws;
using std::noskipws;

/* Project namespace. */
namespace JB {

const int IGNORE_SZ = 65535;	
	
// ---------------------------------------------------------------------
// Pix_256 Record structure for a pixel with sample values 0 to 255. 
// ---------------------------------------------------------------------
struct Pix_256 {
	unsigned char r;
	unsigned char g;
	unsigned char b;
};

// ---------------------------------------------------------------------
// PPM_File: Record structure for a PPM file. 
// ---------------------------------------------------------------------
class PPM_File {
public:

	PPM_File(string fname = ""): file_name(fname), pixels(NULL) {}
	~PPM_File() { if (pixels != NULL) delete [] pixels; }

	string file_name;
	int width, height;
	int maxval;
	struct Pix_256* pixels;
};

// ---------------------------------------------------------------------
// PPM_Reader: Used for reading in PPM image files.
// ---------------------------------------------------------------------
class PPM_Reader {
public:	
	void m_read_ppm(PPM_File &);
	
	
protected:
	void m_read_header(ifstream &, PPM_File &);
	
	bool m_magicnum(ifstream &);
	bool m_dimension(ifstream &, int &);
	bool m_maxval(ifstream &, int &);
	
	void m_read_pixels(ifstream &, PPM_File &);
	
	bool m_comment(string &);
	bool m_decimal(string &);
};

// ---------------------------------------------------------------------
// PPM_Writer: Used for writing out PPM image files. 
// ---------------------------------------------------------------------
class PPM_Writer {
public:
	void m_write_ppm(PPM_File &, string &);
	
	
protected:
	
};
	
}

#endif

