/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.datasetup;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.FileUtils;
import ps.util.ModuleProperties;
import ps.util.Report;
import s390.Runner;
import s390.fits.FITSColumn;
import s390.fits.FITSTable2sql;
import s390.fits.FITSBinaryFile;

/**
 * Batch program to load DR10Q quasar catalog FITS file to
 * relational database for easier processing.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Qcat2db implements Runnable {

	/**
	 * Program main entry point
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		
		Runner.run(Qcat2db.class);
		
	}
	
	// Dependencies
	
	/**
	 * Properties/parameters
	 */
	@Inject ModuleProperties props;

	
	/**
	 * SQL database access
	 */
	@Inject SQL sql;

	
	/**
	 * Progress reporting to console
	 */
	@Inject Report report;

	
	/**
	 * Main program
	 */
	@Override
	public void run() {
		
		String strQCat = props.get("DR10Q");
		strQCat = FileUtils.expandUserHome(strQCat);
		
		try {
			
			load(strQCat);
			
		} catch (IOException ex) {
			
			// Convert to runtime exception
			throw new RuntimeException(ex);
			
		}
		
	}

	
	/**
	 * Load the quasar catalogue to database
	 * @param fileName the filename of the DR10Q FITS format catalogue
	 * @throws IOException if any I/O error occurs during processing.
	 */
	public void load(String fileName) throws IOException {

		report.info("Start");
		
		sql.update("set autocommit off");

		/*
		 *  Override the OBJ_ID column during conversion to make it the primary key of the database
		 *  table, and to convert from a string to long integer representation.
		 *  Override the PSFMAG and EXINCTION array columns to only accept the 4th iteration,
		 *  corresponding to the i-band value of the u,g,r,i,z photometric bands.
		 */
		List<FITSColumn> columnOverrides = Arrays.asList(
				
				new FITSColumn.Type("OBJ_ID", "bigint primary key") {
					@Override public Object convert(Object origValue) {return Long.valueOf((String)origValue);}
				},
				
				new FITSColumn.Iter("PSFMAG", 4), new FITSColumn.Iter("EXTINCTION", 4) 
		);
		
		// Create a list of column names for conversion by splitting the items in csvQcat
		List<String> columnList = Arrays.asList(csvQcat.split(",")).stream()
				.map(s -> s.trim()).collect(Collectors.toList());
		
		/*
		 * Open FITS file and create helper object to shovel quasar catalog onto the database.
		 * We pass it the desired db table name (QCAT), a binary table from the FITS file,
		 * a list of column names we want to transfer from table to database, and an array
		 * of converters for columns that need special treatment during conversion.
		 */
		FITSBinaryFile fits = new FITSBinaryFile(new FileInputStream(fileName));
		FITSTable2sql f2Qcat = new FITSTable2sql("QCAT", fits.getTable(1),
				columnList, columnOverrides);

		// Create QCAT table using definition acquired from helper object
		report.info("Create QCAT table");
		sql.update(f2Qcat.getTableDef());
		
		// Load the FITS data
		report.info("Load quasar catalogue");
		f2Qcat.loadSQLData(sql);
		
		// Commit and finish
		sql.update("commit");		
		report.info("Finished");
		
	}


	/**
	 * Comma-separated list of columns we load from the quasar catalog FITS file
	 */
	private final String csvQcat = "OBJ_ID, RA, DEC, Z_VI, MI, BAL_FLAG_VI, PSFMAG, EXTINCTION, PLATE, MJD, FIBERID";
	

	/**
	 * Comma-separated list of all columns from the quasar catalog FITS file.
	 * (Unused, for info only).
	 */
	@SuppressWarnings("unused")
	private final String csvQcat_alt =
			"SDSS_NAME, RA, DEC, THING_ID, PLATE, MJD, FIBERID, Z_VI, Z_PIPE, ERR_ZPIPE, ZWARNING, Z_PCA, ERR_ZPCA, PCA_QUAL, "
			+ "Z_CIV, Z_CIII, Z_MGII, SDSS_MORPHO, BOSS_TARGET1, ANCILLARY_TARGET1, ANCILLARY_TARGET2, SDSS_DR7, PLATE_DR7, "
			+ "MJD_DR7, FIBERID_DR7, UNIFORM, ALPHA_NU, SNR_SPEC, SNR_1700, SNR_3000, SNR_5150, FWHM_CIV, BHWHM_CIV, "
			+ "RHWHM_CIV, AMP_CIV, REWE_CIV, ERR_REWE_CIV, FWHM_CIII, BHWHM_CIII, RHWHM_CIII, AMP_CIII, REWE_CIII, "
			+ "ERR_REWE_CIII, FWHM_MGII, BHWHM_MGII, RHWHM_MGII, AMP_MGII, REWE_MGII, ERR_REWE_MGII, BAL_FLAG_VI, BI_CIV, "
			+ "ERR_BI_CIV, AI_CIV, ERR_AI_CIV, CHI2TROUGH, NCIV_2000, VMIN_CIV_2000, VMAX_CIV_2000, NCIV_450, VMIN_CIV_450, "
			+ "VMAX_CIV_450, REW_SIIV, REW_CIV, REW_ALIII, RUN_NUMBER, PHOTO_MJD, RERUN_NUMBER, COL_NUMBER, FIELD_NUMBER, "
			+ "OBJ_ID, MI, DGMI, HI_GAL, "
			// iterated fields, one per 5 filter bands
			+ "PSFFLUX, IVAR_PSFFLUX, PSFMAG, ERR_PSFMAG, TARGET_FLUX, EXTINCTION, EXTINCTION_RECAL";


}
