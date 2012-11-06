#parallel-image-filters
This directory contains a project written for a parallel programming course.
Two programs can be built: afilter, and threshold. The programs have been tested
in Ubuntu Linux 11.04. Both programs take an input image in PPM format, apply a filter
to it, and write out a new image resulting from the filter. The filters are written
in a parallel fashion to take advantage of multiple cores/processors available
on the user's system. This is done with the Message Passing Interface (MPI)
library.
* __afilter__: The afilter program implements an "averaging filter". The averaging
filter modifies each pixel in the input image by changing its red/green/blue (RGB)
values to the average RGB values of the surrounding pixels. This results in a 
blurring effect.
* __threshold__: The threshold program implements a "threshold filter". The program
calculates the average intensity of the input image, and colors a pixel in the 
output image (a) white, if its intensity is lower than the average, or (b) black, if
its intensity is greater than the average.  

__Requirements__:
* Linux OS (tested on Ubuntu 11.04)
* An implementation of the Message Passing Interface (MPI) API.
    * The OpenMPI implementation is available [here](http://www.open-mpi.org/).
    * Add the OpenMPI "bin" directory to your PATH environment variable
    * Add the OpenMPI "lib" directory to your LD\_LIBRARY\_PATH variable

## Using the Averaging Filter Program
To build the afilter program, navigate to the Makefile directory and enter:  
`make afilter`.  

The afilter program can be executed with the following command:  
`./afilter <input> <output> <x-radius> <y-radius>`  
* __input__: This parameter specifies the input PPM image to filter.
* __output__: This parameter specifies what to name the output image resulting from
the filter.
* __x-radius__: Specifies the x-radius of the rectangle of pixels to consider when
calculating the average RGB values of a pixel's neighbours.
* __y-radius__: Specifies the y-radius of the rectangle of pixels to consider when
calculating the average RGB values of a pixel's neighbours.
  
__Running with OpenMPI__:  
To run afilter with OpenMPI, use the following command:  
`mpirun -n <# processes> ./afilter <input> <output> <x-radius> <y-radius>`  
* __# processes__: A parameter telling OpenMPI how many processes should be used to
execute the averaging filter.
  
__Example 1__: Take input1.ppm and, using 4 parallel processes, create a blurred
copy of input1.ppm called output1.ppm. Each output pixel will be the average of 
the input pixels surrounding it in a 10x10 pixel rectangle.  
`mpirun -n 4 ./afilter input1.ppm output1.ppm 10 10`  


## Using the Thresholding Filter Program
To build the thresholding program, navigate in a terminal to the Makefile directory,
and enter:  
`make threshold`  

The threshold program can be executed with the following command:  
`./threshold <input> <output>`  
* __input__: The input PPM image.
* __output__: The image resulting from thresholding the input's pixels.
  
__Running with OpenMPI__:  
To run threshold with OpenMPI, use the following command:  
`mpirun -n <#_processes> ./threshold <input> <output>`  
* __# processes__: A parameter telling OpenMPI how many processes should be used to
execute the averaging filter.  

__Example 1__: Take input image input.ppm, threshold it, and output the resulting
image in output.ppm. Do this using 8 processes.  
`mpirun -n 8 ./threshold input.ppm output.ppm`  




