/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process.datasetup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ps.db.SQL;
import ps.util.FileUtils;
import ps.util.ModuleProperties;
import ps.util.Report;
import ps.util.TextUtils;
import s390.Quasar;
import s390.Runner;
import s390.fits.FITSBinaryFile;
import s390.fits.FITSTable;
import s390.fits.FITSTable2sql;
import s390.sdss.Naming;
import s390.sdss.spec.impl.SpectrumDbWriterImpl;
import s390.sdss.spec.impl.SpectrumImpl;

/**
 * Load spectrum database file from individual SDSS spectrum FITS files.
 * The output is a single file containing all spectra which is more performant
 * to read than the hundreds of thousands of individual spectrum FITS files.
 * A second output contains emission line information in csv format, consolidated
 * from the individual spectrum FITS files.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class Spectra2Database implements Runnable {

	/**
	 * Program main entry point
	 * 
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {
		
		Runner.run(Spectra2Database.class);
		
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
		
		// Get the path to the new spectrum database file which will be output.
		String strLite = props.get("useLite");
		boolean lite = !TextUtils.nullOrEmpty(strLite) && Boolean.valueOf(strLite);
		String strIndexProp = lite? "spectrumDbLite" : "spectrumDb";
		String strIndexPath = props.get(strIndexProp);
		strIndexPath = FileUtils.expandUserHome(strIndexPath);
		Path indexPath = FileSystems.getDefault().getPath(strIndexPath);
		
		// Get the path to the output csv file which will contain spzline information
		String strSpzlinePath = props.get("spzlineCsv");
		strSpzlinePath = FileUtils.expandUserHome(strSpzlinePath);

		// Get the directory containing the individual SDSS spectrum FITS files.
		String strSpectrumFITS = props.get("dirSpectrumFITS");
		strSpectrumFITS = FileUtils.expandUserHome(strSpectrumFITS);
		QuasarBinaryData.dirSpecFiles = new File(strSpectrumFITS);
		
		try {
			// Main processing
			createSpecIndex(indexPath, strSpzlinePath, lite);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	

	/**
	 * Creates a new spectrum database file and new emission line csv file.
	 * 
	 * @param indexPath
	 *            the path to the new spectrum database file.
	 * @param strSpzlinePath
	 *            the path to the emission line csv file.
	 * @param lite
	 *            whether to use the "lite" version of the spectrum database
	 * 
	 * @throws IOException
	 *             if any I/O exception occurs during processing.
	 */
	public void createSpecIndex(Path indexPath, String strSpzlinePath, boolean lite) throws IOException {

		report.info("Begin ...");

		// Get a list of quasars sorted by id.
		List<QuasarBinaryData> quasars = Quasar.queryAll(sql).map(QuasarBinaryData::new)
				.sorted(Comparator.comparingLong(q -> q.getQuasar().getObjID()))
				.collect(Collectors.toList());
		
		// Create the new output spectrum database file.
		SpectrumDbWriterImpl specDb = new SpectrumDbWriterImpl(indexPath);
		
		// Create the new output emission line csv file and print column headings to it.
		PrintWriter pw = new PrintWriter(new FileWriter(strSpzlinePath));
		pw.println("OBJ_ID," + kColSpzline);

		// Use the first quasar in the list to get metadata about the spzline FITS binary table.
		QuasarBinaryData q0 = quasars.get(0);
		FITSTable2sql dataSpzline = new FITSTable2sql("", q0.getSpzLineTable());
		
		// Counters
		int nq = 0;
		int totalRows = 0;

		// Process list of quasars
		for (final QuasarBinaryData qbd : quasars) {
			
			Quasar q = qbd.getQuasar();
			
			// Counters and progress reporting
			nq++;
			if (nq % 1000 == 0) {
				report.info("%s",nq);
			}
			
			// Get the FITS binary table containing coadded spectrum
			FITSTable tableCoadd = qbd.getCoaddTable();
			totalRows += tableCoadd.getNRows();

			// Create a new Spectrum object and add it to the output database
			SpectrumImpl spec = lite ? new SpectrumImpl(q.getObjID(),
					tableCoadd) : new SpectrumImpl(q.getObjID(), q.getPlate(),
					q.getMJD(), q.getFiber(), tableCoadd);

			specDb.add(spec);
			
			// A consumer for an spzline row which outputs it as a csv row
			Consumer<Object[]> consumerSpzline = (data) -> {
				
				// Check linez and linezErr for sentinel values that we ignore
				float linez = (Float)data[5];
				float linezErr = (Float)data[6];
				
				// Print if wanted
				if ((linez != 0) && (linezErr != -1)) {
					pw.print(q.getObjID());
					for (Object o : data) {
						pw.print(',');
						pw.print(o);
					}
					pw.println();
				}
				
			};
			
			// Get the spzline information and output csv values
			dataSpzline.loadData(consumerSpzline, qbd.getSpzLineTable());
			
			// "Close" quasar to allow for garbage collection of FITS tables
			qbd.close();
						
		}
		
		// Close the output spectrum database
		specDb.finish();
		
		// Close the output emission line csv file
		pw.close();

		// Final reporting
		report.info("End: %s quasars, %s rows", nq, totalRows);
		
	}
	

	/**
	 * A helper class for getting FITS binary table data for a quasar.
	 *
	 */
	static private class QuasarBinaryData {
		
		/**
		 * Directory for SDSS DR10Q raw spectrum FITS files
		 */
		static File dirSpecFiles;
		
		
		/**
		 * Access to FITS binary table data
		 */
		FITSBinaryFile tables;
		
		
		/**
		 * Associated quasar.
		 */
		Quasar q;
		
		
		/**
		 * Constructor. Construct a quasar from a relational database row.
		 * @param r SQLResult containing database row
		 */
		QuasarBinaryData(Quasar q) {
			this.q = q;
		}
		
		
		/**
		 * Get associated quasar.
		 * 
		 * @return quasar
		 */
		Quasar getQuasar() {
			return q;
		}
		
		/**
		 * Open raw FITS spectrum file to access binary table data.
		 * @return a set of FITS tables
		 * @throws FileNotFoundException if the raw spectrum file is not found.
		 */
		private FITSBinaryFile getFITSTables() throws FileNotFoundException {
			if (tables == null) {
				String specFileName = Naming.getSpecFileName(q.getPlate(), q.getMJD(), q.getFiber());
				File specFile = new File(dirSpecFiles, specFileName);
				tables = new FITSBinaryFile(new FileInputStream(specFile));
			}
			return tables;
		}
		
		
		/**
		 * Get the COADD binary FITS table for this quasar's spectrum.
		 * 
		 * @return COADD binary table
		 * @throws FileNotFoundException
		 *             if the spectrum file cannot be found
		 */
		FITSTable getCoaddTable() throws FileNotFoundException {
			return getFITSTables().getTable("COADD");
		}
		
		
		/**
		 * Get the SPZLINE binary FITS table for this quasar's spectrum.
		 * 
		 * @return SPZLINE binary table
		 * @throws FileNotFoundException
		 *             if the spectrum file cannot be found
		 */
		FITSTable getSpzLineTable() throws FileNotFoundException {
			return getFITSTables().getTable("SPZLINE");
		}
		
		
		/**
		 * Nullify tables to allow garbage collection
		 */
		void close() {
			tables = null;
		}
		
	}
	
	
	/**
	 * List of spzline (SDSS emission line data) FITS binary table columns.
	 */
	private final static String kColSpzline =
			"PLATE,MJD,FIBERID,LINENAME,LINEWAVE,LINEZ,LINEZ_ERR,LINESIGMA,LINESIGMA_ERR," +
			"LINEAREA,LINEAREA_ERR,LINEEW,LINEEW_ERR,LINECONTLEVEL,LINECONTLEVEL_ERR," +
			"LINENPIXLEFT,LINENPIXRIGHT,LINEDOF,LINECHI2";

}
