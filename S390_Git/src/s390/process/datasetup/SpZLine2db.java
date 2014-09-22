/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.datasetup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.FileUtils;
import ps.util.ModuleProperties;
import ps.util.Report;
import s390.Quasar;
import s390.Runner;
import s390.fits.FITSColumn;
import s390.fits.FITSTable2sql;

/**
 * Batch program to load DR10Q quasar catalog FITS file to
 * relational database for easier processing. This program
 * handles the SPZLINE binary FITS table containing emission
 * line measurements.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class SpZLine2db implements Runnable {

	/**
	 * Program main entry point
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		
		Runner.run(SpZLine2db.class);
		
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
	 * Main program.
	 * 
	 */
	@Override
	public void run() {
		
		// Get the directory containing the individual SDSS spectrum FITS files.
		String strSpectrumFITS = props.get("dirSpectrumFITS");
		strSpectrumFITS = FileUtils.expandUserHome(strSpectrumFITS);
		QuasarBinaryData.dirSpecFiles = new File(strSpectrumFITS);
		
		try {
			// Main processing
			createSpZLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	

	/**
	 * Creates the SPZLINE database table from FITS file data.
	 * 
	 * @throws IOException
	 *             if any I/O exception occurs during processing.
	 */
	public void createSpZLine() throws IOException {

		report.info("Begin ...");

		// Get a list of quasars sorted by id.
		List<QuasarBinaryData> quasars = Quasar.queryAll(sql).map(QuasarBinaryData::new)
				.sorted(Comparator.comparingLong(q -> q.getQuasar().getID()))
				.collect(Collectors.toList());
		
		// Get list of SPZLINE columns
		List<String> cols = Arrays.asList(kColSpzline.split(",")).stream()
				.map(s -> s.trim()).collect(Collectors.toList());
		// Remove plate, mjd, fiber columns and add obj_id
		cols.remove("PLATE");
		cols.remove("MJD");
		cols.remove("FIBERID");
		cols.add(0, "OBJ_ID");
		
		// OBJ_ID id column converter
		FITSColumn.ID idCol = new FITSColumn.ID("OBJ_ID", "BIGINT NOT NULL");
		
		// Only accept rows with LINEZ_ERR != -1;
		FITSColumn.Named lineZErrCol = new FITSColumn.Named("LINEZ_ERR") {
			@Override
			public boolean acceptRow(Object v) {return ((Float)v) != -1;}
		};
		
		// Use the first quasar in the list to get metadata about the spzline FITS binary table.
		QuasarBinaryData q0 = quasars.get(0);
		
		// Setup table metadata
		FITSTable2sql dataSpzline = new FITSTable2sql("SPZLINE",
				q0.getSpzLineTable(), cols, Arrays.asList(idCol, lineZErrCol));
				
		// Create SPZLINE table using definition acquired from helper object
		report.info("Create SPZLINE table \n%s", dataSpzline.getTableDef());
		sql.update(dataSpzline.getTableDef());
		
		// Load the FITS data
		report.info("Load SPZLINE data");

		// Counters
		int nq = 0;

		// Process list of quasars
		for (final QuasarBinaryData qbd : quasars) {
			
			Quasar q = qbd.getQuasar();
			
			// Counters and progress reporting
			nq++;
			if (nq % 1000 == 0) {
				report.info("%s",nq);
			}
			
			idCol.setID(q.getID());
			
			// Load the spzline information to database
			dataSpzline.loadSQLData(sql, qbd.getSpzLineTable());
			
			// "Close" quasar to allow for garbage collection of FITS tables
			qbd.close();
						
		}
		
		// Final reporting
		report.info("End: %s quasars", nq);
		
	}
	

	/**
	 * List of spzline (SDSS emission line data) FITS binary table columns.
	 */
	private final static String kColSpzline =
			"PLATE,MJD,FIBERID,LINENAME,LINEWAVE,LINEZ,LINEZ_ERR,LINESIGMA,LINESIGMA_ERR," +
			"LINEAREA,LINEAREA_ERR,LINEEW,LINEEW_ERR,LINECONTLEVEL,LINECONTLEVEL_ERR," +
			"LINENPIXLEFT,LINENPIXRIGHT,LINEDOF,LINECHI2";

}
