/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.process;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import ps.db.SQL;
import ps.util.KeyValuePair;
import ps.util.Report;
import s390.EmissionLine;
import s390.Quasar;
import s390.Runner;

/**
 * Analyse line information on SpZLine.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class LineAnalysis implements Runnable {

	public static void main(String[] args) {
		Runner.run(LineAnalysis.class);
	}
	
	
	// Dependencies
	
	/**
	 * SQL for database processing
	 */
	@Inject SQL sql;
	
	
	/**
	 * Console output
	 */
	@Inject Report report;
	
	
	/**
	 * Parameter: output directory.
	 */
	@Inject @Named("dirOut") File dirOut;
	
		
	/**
	 * Main program
	 */
	@Override
	public void run() {

		report.info("Begin...");
		
		// Create a map of good quasars by unique id
		Map<Long, Quasar> qmap = Quasar.queryGood(sql).collect(
				Functions.mapBy(Quasar::getID));

		report.info("quasars %s", qmap.size());

		// Get a list of emission lines for good quasars
		List<EmissionLine> lineList = EmissionLine.queryAll(sql)
				.filter(emline -> qmap.containsKey(emline.getID()))
				.peek(emline -> emline.setQuasar(qmap.get(emline.getID())))
				.collect(Collectors.toList());

		report.info("quasars w/ lines %s", lineList.size());
		
		doLineAnalysis(lineList);
		
	}


	/**
	 * Analyse lines.
	 * 
	 * @param lineList
	 *            list of emission lines for good quasars
	 */
	void doLineAnalysis(List<EmissionLine> lineList) {
		
		// Reorganise into map of emission lines by wavelength
		Map<Double, List<EmissionLine>> lineWLMap = lineList.stream()
				.collect(Collectors.groupingBy(EmissionLine::getWavelength));
		
		report.info("distinct lines %s", lineWLMap.size());

		lineWLMap
				.entrySet()
				.stream()
				.sorted(Map.Entry.<Double, List<EmissionLine>> comparingByKey())
				.forEach(
						e -> report.info("%4.2f (%s) -> %s", e.getKey(), e
								.getValue().get(0).getLineName(), e.getValue()
								.size()));

		report.info("...end");
		
	}
	

}
