/**
 * @file 	jb_ppm.cc
 * @author	Joram Benham <x2008kui@stfx.ca>
 * @version Feb. 17, 2012
 * 
 * @section DESCRIPTION
 * 
 * This is the implementation file for classes dealing with PPM image.
 */

#include "jb_ppm.h"
#include <iostream>

namespace JB {
	

/* *********************************************************************
 * PPM_Reader: public methods
 * ********************************************************************/

/**
 * m_read_ppm reads a PPM file into the Pix_256 array of the method
 * argument. The file path is taken from the method argument.
 * 
 * @param ppm A PPM_File object containing the path of the desired
 * 			PPM file.
 * @return void Nothing is returned; the PPM data is stored in the 'ppm'.
 */
void
PPM_Reader::m_read_ppm(PPM_File &ppm) 
{
	ifstream infile;
	
	// Try to open the file, throw an exception on failure.
	infile.open(ppm.file_name.c_str(), ifstream::in);
		
	if (infile.fail()) throw ios_base::failure("Failed to open file");
	infile >> std::skipws;
	
	// Extract the data from the file.
	m_read_header(infile, ppm);		
	m_read_pixels(infile, ppm);
	
	infile.close();	
}
	
	
/* *********************************************************************
 * PPM_Reader: private/protected methods
 * ********************************************************************/	

/**
 * m_read_header attempts to read in the header section of a PPM file.
 * If no magic number is found, the method continues to try to read in 
 * the header. However if a width, height, or max value cannot be found,
 * an exception is thrown.
 * 
 * @param fin The input file stream with the PPM file opened.
 * @param ppm The PPM_File object to store data read in.
 * @return void Nothing is returned.
 */
void
PPM_Reader::m_read_header(ifstream &fin, PPM_File &ppm) 
{

	m_magicnum(fin);
	
	if (m_dimension(fin, ppm.width) == false) {
		throw ios_base::failure("No width found in PPM header");
	}
	
	if (m_dimension(fin, ppm.height) == false) {
		throw ios_base::failure("No height found in PPM header");
	}
	
	if (m_maxval(fin, ppm.maxval) == false) {
		throw ios_base::failure("No maxval found in PPM header");
	}

}


/**
 * m_magicnum attempts to find and read in the magic number of the PPM
 * file. If is is not found, the 'get' pointer of the ifstream object
 * is reset.
 * 
 * @param fin The ifstream object with the PPM file opened.
 * @param bool Return true if the magic number was found; false otherwise. 
 */
bool
PPM_Reader::m_magicnum(ifstream &fin) 
{
	string token;
	
	fin.seekg(0, ios::beg);
	fin >> skipws;
	for (fin >> token; m_comment(token); fin.ignore(IGNORE_SZ, '\n'), fin >> token) {}
	
	// If the magic number was not found, reset the get pointer.
	if (token == "P6") {
		return true;
	}
	else {
		fin.seekg(0, ios::beg);
		return false;
	}
}

/**
 * m_dimension attempts to read in a dimension of the PPM file.
 * 
 * @param fin The ifstream object with the PPM file opened.
 * @param store Where the dimension is stored if found.
 * @return bool Return true if found; false otherwise.
 */
bool
PPM_Reader::m_dimension(ifstream &fin, int &store) 
{
	string token;
	
	fin >> skipws;
	for (fin >> token; m_comment(token); fin.ignore(IGNORE_SZ, '\n'), fin >> token) {}
	
	// If the width was not found, return false.
	if (m_decimal(token)) {
		store = std::atoi(token.c_str());
		return true;
	}
	else {
		return false;
	}
}


/**
 * m_maxval attempts to read in the maximum color value of a PPM.
 * 
 * @param fin The ifstream object with the PPM file opened.
 * @param maxval Reference to where the max. val should be stored.
 * @return bool Return true if found; false otherwise.
 */
bool
PPM_Reader::m_maxval(ifstream &fin, int &maxval) 
{
	string token;
	int intval;
	
	fin >> skipws;
	for (fin >> token; m_comment(token); fin.ignore(IGNORE_SZ, '\n'), fin >> token) {}
	
	// Check for valid maximum color value.
	if (m_decimal(token)) {
		
		intval = std::atoi(token.c_str());
		
		if (intval > 0 && intval < 65536) {
			maxval = intval;
			return true;
		}
		
	}
	else {
		return false;
	}	
}

/**
 * m_read_pixels attempts to read in the pixel information stored in the
 * PPM image.
 * 
 * @param fin The ifstream object with the PPM file opened.
 * @param ppm The PPM_File object to store the pixel information in.
 * @return void Nothing is returned.
 */
void
PPM_Reader::m_read_pixels(ifstream &fin, PPM_File &ppm) 
{
	long num_pix;
	
	// Get rid of leading spaces.
	while (std::isspace(fin.peek())) { fin.ignore(); }
	
	// Do not skip white space.
	fin >> noskipws;
	
	// Read in the raster.
	num_pix = ppm.height * ppm.width;
	ppm.pixels = new Pix_256[num_pix];
	
	for (int i = 0; i < num_pix && !fin.eof(); i++) {
		fin >> ppm.pixels[i].r >> ppm.pixels[i].g >> ppm.pixels[i].b;
	}
	
	fin >> skipws;
}


/**
 * m_comment checks if a string begins with a PPM line comment.
 * @param str The string to check.
 * @param bool returns true iff the first character is a hash. 
 */
bool
PPM_Reader::m_comment(string &str) 
{
	return (str[0] == '#');
}


/**
 * m_decimal checks if a string is a decimal integer.
 * @param str The string to check.
 * @return bool Return true if the string is a decimal integer.
 */
bool
PPM_Reader::m_decimal(string &str) 
{
	for (int i = 0; i < str.length(); i++) {
		if (std::isdigit(str[i]) == false) return false;
	}
	
	return true;
}
	

/* *********************************************************************
 * PPM_Writer: public methods
 * ********************************************************************/
/**
 * m_write_ppm attempts to write PPM header and pixel data out to a file.
 * @param ppm The PPM_File object with the pixel RGB values.
 * @param filename The path of the output file name.
 * @return void Nothing is returned; exceptions are thrown on error.
 */
void 
PPM_Writer::m_write_ppm(PPM_File &ppm, string &filename) 
{
	ofstream fout;
	long num_pix = ppm.width * ppm.height;

	// Open the file.
	fout.open(filename.c_str());
	if (fout.fail()) {
		throw ios_base::failure("Failed to open output file");
	}
	
	// Output PPM header.
	fout << "P6\n";
	fout << ppm.width << " " << ppm.height << "\n";
	fout << ppm.maxval << "\n";
	
	// Output pixels.
	for (int i = 0; i < num_pix; i++) {
		fout.write((char*)(&(ppm.pixels[i].r)), 1);
		fout.write((char*)(&(ppm.pixels[i].g)), 1);
		fout.write((char*)(&(ppm.pixels[i].b)), 1);
	}
	
	fout.close();
}
	
}
