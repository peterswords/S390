/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.sdss.spec.impl;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import s390.fits.FITSTable;
import s390.sdss.spec.SpecPixel;
import s390.sdss.spec.Spectrum;

/**
 * Implementation of Spectrum.
 * 
 * See the SDSS <a href=
 *      "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
 *      >COADD data model</a> for further information.
 * 
 * This implementation can be initialised either as a "lite" version of the spectrum COADD data,
 * or the full version. The lite version stores only object ID, and flux and loglam (wavelength)
 * arrays. See the COADD data model for information on other fields.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class SpectrumImpl implements Spectrum {

	/**
	 * "Lite" indicator. The lite version of a spectrum omits all information
	 * except the object id of the object, and the flux and wavelength of each
	 * pixel.
	 */
	private boolean lite;
	
	/**
	 * Unique object id.
	 */
	private long objID;
	
	/**
	 * Number of pixels in spectrum.
	 */
	private int nSamples;
	
	/**
	 * Pixel flux. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 */
	private float[] flux;
	
	/**
	 * Pixel wavelength. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 */
	private float[] loglam;
	
	
	// Other data from the SDSS COADD data model
	// These are not stored in the "lite" version.
	
	
	/**
	 * Inverse variance of flux. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 * 
	 */
	private float[] ivar;

	
	/**
	 * AND mask. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 */
	private int[] andmask;

	
	/**
	 * OR mask. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 */
	private int[] ormask;
	
	
	/**
	 * Wavelength dispersion. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 */
	private float[] wdisp;

	
	/**
	 * Subtracted sky flux level. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 */
	private float[] sky;

	
	/**
	 * SDSS pipeline flux used for red shift estimation. See the SDSS <a href=
	 * "http://data.sdss3.org/datamodel/files/BOSS_SPECTRO_REDUX/RUN2D/spectra/PLATE4/spec.html"
	 * >COADD data model</a> for further information.
	 */
	private float[] model;


	/**
	 * SDSS drilled plate number.
	 */
	private int plate;
	
	
	/**
	 * Modified Julian data of exposure.
	 */
	private int mjd;
	
	
	/**
	 * Fiber number within plate.
	 */
	private int fiber;

	
	/**
	 * Construct spectrum.
	 * @param objID unique object ID.
	 * @param plate see COADD data model description.
	 * @param mjd see COADD data model description.
	 * @param fiber see COADD data model description.
	 * @param coadd see COADD data model description.
	 */
	public SpectrumImpl(long objID, int plate, int mjd, int fiber, FITSTable coadd) {
		this.objID = objID;
		this.plate = plate;
		this.mjd = mjd;
		this.fiber = fiber;
		flux = (float[])coadd.getColumn("flux");
		loglam = (float[])coadd.getColumn("loglam");
		ivar = (float[])coadd.getColumn("ivar");
		andmask = (int[])coadd.getColumn("and_mask");
		ormask = (int[])coadd.getColumn("or_mask");
		wdisp = (float[])coadd.getColumn("wdisp");
		sky = (float[])coadd.getColumn("sky");
		model = (float[])coadd.getColumn("model");
		this.nSamples = flux.length;
	}
	
	
	SpectrumImpl() {
	}
	
	
	@Override
	public long getObjID() {
		return objID;
	}

	/**
	 * Fail if this is the lite version of the spectrum.
	 * @throws RuntimeException if this is the lite version.
	 */
	private void nolite() {
		if (lite) {
			throw new RuntimeException("Field not available from lite spectrum");
		}
	}
	
	
	/**
	 * Get SDSS plate number.
	 * @return plate number
	 */
	private int getPlate() {
		nolite();
		return plate;
	}


	/**
	 * Get exposure Modified Julian Date
	 * @return mjd date
	 */
	private int getMJD() {
		nolite();
		return mjd;
	}

	
	/**
	 * Get fiber id within plate.
	 * @return fiber id.
	 */
	private int getFiber() {
		nolite();
		return fiber;
	}

	
	/**
	 * Get number of pixels in spectrum.
	 * @return number of pixels.
	 */
	@Override
	public int getNumPixels() {
		return nSamples;
	}

	
	/**
	 * Get an iterator over the pixels of the spectrum
	 * @return the iterator.
	 */
	@Override
	public Iterable<SpecPixel> getPixels() {
		return new Iterable<SpecPixel>() {
			
			@Override
			public Iterator<SpecPixel> iterator() {
				return new Iterator<SpecPixel>() {

					int n = 0;
					
					@Override
					public boolean hasNext() {
						return n < nSamples;
					}

					@Override
					public SpecPixel next() {
						SpecPixel result = new SpecPixel() {

							int j = n;
							@Override public float getFlux()    {return flux[j];}
							@Override public float getLoglam()  {return loglam[j];}
							@Override public float getIvar()    {nolite(); return ivar[j];}
							@Override public int   getAndMask() {nolite(); return andmask[j];}
							@Override public int   getOrMask()  {nolite(); return ormask[j];}
							@Override public float getWdisp()   {nolite(); return wdisp[j];}
							@Override public float getSky()     {nolite(); return sky[j];}
							@Override public float getModel()   {nolite(); return model[j];}
							
						};
						n++;
						return result;
					}
					
				};
			}
		};
	}

	
	/**
	 * Get a stream of pixels in the spectrum.
	 * @return pixel stream.
	 */
	@Override
	public Stream<SpecPixel> stream() {
		return StreamSupport.stream(getPixels().spliterator(), false);
	}

	
	/**
	 * Write the spectrum to a byte buffer.
	 * @param lite indicator if lite version of spectrum is to be written.
	 * @return a byte buffer containing the spectrum
	 */
	ByteBuffer toByteBuffer(boolean lite) {
		int pixSize = lite? 8 : 32;
		ByteBuffer buf = ByteBuffer.allocate(8000 * pixSize); // estimated size for 8k pixels
		buf.putLong(getObjID());
		if (!lite) {
	 		buf.putInt(getPlate());
			buf.putInt(getMJD());
			buf.putInt(getFiber());
		}
		buf.putInt(getNumPixels());
		for (SpecPixel s : getPixels()) {
			buf.putFloat(s.getFlux());
			buf.putFloat(s.getLoglam());
			if (!lite) {
				buf.putFloat(s.getIvar());
				buf.putInt(s.getAndMask());
				buf.putInt(s.getOrMask());
				buf.putFloat(s.getWdisp());
				buf.putFloat(s.getSky());
				buf.putFloat(s.getModel());
			}
		}
		return buf;
	}
	
	
	/**
	 * Read a spectrum from a byte buffer.
	 * @param buf the byte buffer from which to read.
	 * @param lite indicates if lite version of spectrum is to be read.
	 * @return the spectrum that has been read.
	 */
	static SpectrumImpl fromByteBuffer(ByteBuffer buf, boolean lite) {
		SpectrumImpl spec = new SpectrumImpl();
		spec.lite = lite;
		spec.objID = buf.getLong();
		if (!spec.lite) {
			spec.plate = buf.getInt();
			spec.mjd = buf.getInt();
			spec.fiber = buf.getInt();
		}
		int n = buf.getInt();
		spec.nSamples = n;
		spec.flux = new float[n];
		spec.loglam = new float[n];
		if (!spec.lite) {
			spec.ivar = new float[n];
			spec.andmask = new int[n];
			spec.ormask = new int[n];
			spec.wdisp = new float[n];
			spec.sky = new float[n];
			spec.model = new float[n];
		}
		for (int i = 0; i < n; i++) {
			spec.flux[i] = buf.getFloat();
			spec.loglam[i] = buf.getFloat();
			if (!spec.lite) {
				spec.ivar[i] = buf.getFloat();
				spec.andmask[i] = buf.getInt();
				spec.ormask[i] = buf.getInt();
				spec.wdisp[i] = buf.getFloat();
				spec.sky[i] = buf.getFloat();
				spec.model[i] = buf.getFloat();
			}
		}
		return spec;
	}
	
}
