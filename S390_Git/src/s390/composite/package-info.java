/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
/**
 * Algorithms for composite spectrum generation.
 * Three types of spectra are defined -- an intermediate spectrum which is
 * used to shift, rebin and normalise a raw input spectrum; an accumulator
 * spectrum which allows many intermediate spectra to be added together;
 * and a composite spectrum derived from the accumulator spectrum after
 * all the inputs have been added.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
package s390.composite;