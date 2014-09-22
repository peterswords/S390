/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import ps.db.SQL;
import ps.db.SQLResult;

/**
 * Represents an emission line of a quasar spectrum from the DR10Q SDSS Quasar catalogue.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class EmissionLine implements Serializable {
	
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Subset of columns from the SDSS Quasar catalog FITS file that are used.
	 */
	public static final String EMLINE_FIELDS =
		"OBJ_ID, LINENAME, LINEWAVE, LINEZ, LINESIGMA, LINEAREA, LINEEW, LINEEW_ERR, LINECONTLEVEL, LINECONTLEVEL_ERR, LINECHI2";


	/**
	 * Map of line names used to intern/canonicalise database line names.
	 */
	static Map<String, String> kLineNames = new HashMap<>();
	
	
	/**
	 * Quasar that this emission line belongs to. Not initially set.
	 */
	private Quasar quasar;

	
	/**
	 * Quasar unique object id
	 */
	private final long obj_id;

	
	/**
	 * Line name
	 */
	private String lineName;
	
	
	/**
	 * Line wavelength
	 */
	private double wavelength;
	
	
	/**
	 * Line red shift
	 */
	private final float linez;
	
	
	/**
	 * Line standard deviation
	 */
	private final float linesigma;
	
	
	/**
	 * Line area
	 */
	private final float linearea;

	
	/**
	 * Line equivalent width.
	 */
	private final float lineew;

	
	/**
	 * Line equivalent width error.
	 */
	private final float lineew_err;

	
	/**
	 * Line continuum level.
	 */
	private final float lineContLevel;

	
	/**
	 * Line continuum level error.
	 */
	private final float lineContLevel_err;

	
	/**
	 * Line chi squared.
	 */
	private final float linechi2;

	
	/**
	 * 
	 * Get a stream of all lines from database.
	 * 
	 * @param sql
	 *            SQL database access object
	 * @return stream of emission lines
	 */
	public static Stream<EmissionLine> queryAll(SQL sql) {
		
		// Set up the query to retrieve full list of quasars		
		String strQuery = String.format(
				"select %s from SPZLINE", EmissionLine.EMLINE_FIELDS);
		
		return sql.query(strQuery).stream().map(EmissionLine::new);
		
	}

	
	/**
	 * Get a comparator to sort/compare lines by quasar id.
	 * 
	 * @return comparator
	 */
	public static Comparator<EmissionLine> comparingByID() {
		
		return Comparator.comparing(EmissionLine::getID);

	}

	
	/**
	 * Initialize an emission line from relational database.
	 * Line names are canonicalised via a map.
	 * @param r an sql query result
	 */
	public EmissionLine(SQLResult r) {

		obj_id = r.get("OBJ_ID");
		String ln = r.get("LINENAME");
		// Canonicalise
		String cur = kLineNames.putIfAbsent(ln, ln);
		lineName = cur == null? ln : cur;
		wavelength = r.get("LINEWAVE");
		linez = r.get("LINEZ");
		linesigma = r.get("LINESIGMA");
		linearea = r.get("LINEAREA");
		lineew = r.get("LINEEW");
		lineew_err = r.get("LINEEW_ERR");
		lineContLevel = r.get("LINECONTLEVEL");
		lineContLevel_err = r.get("LINECONTLEVEL_ERR");
		linechi2 = r.get("LINECHI2");
		
	}

	
	/**
	 * Override equals() to be compatible with comparison by object id
	 */
	@Override
	public boolean equals(Object obj) {
		
		return (this == obj) || ((obj instanceof EmissionLine) && ((EmissionLine)obj).getID() == getID());

	}
	

	/**
	 * Override hashCode() to be compatible with comparison by object id
	 */
	@Override
	public int hashCode() {
		
		return Long.hashCode(getID());
		
	}
	
	
	/**
	 * Get quasar unique object id
	 * @return object id
	 */
	public long getID() {return obj_id;}


	/**
	 * @return the line name
	 */
	public String getLineName() {
		return lineName;
	}

	
	/**
	 * @return the wavelength
	 */
	public double getWavelength() {
		return wavelength;
	}


	/**
	 * @return the line red shift
	 */
	public float getLinez() {
		return linez;
	}


	/**
	 * @return the line standard deviation
	 */
	public float getLinesigma() {
		return linesigma;
	}


	/**
	 * @return the line area
	 */
	public float getLinearea() {
		return linearea;
	}


	/**
	 * @return the line equivalent width
	 */
	public float getEquivalentWidth() {
		return lineew;
	}


	/**
	 * @return the line equivalent width error
	 */
	public float getEquivalentWidthErr() {
		return lineew_err;
	}


	/**
	 * @return the line continuum level
	 */
	public float getLineContLevel() {
		return lineContLevel;
	}


	/**
	 * @return the line continuum level error
	 */
	public float getLineContLevelErr() {
		return lineContLevel_err;
	}


	/**
	 * @return the line chi squared
	 */
	public float getLinechi2() {
		return linechi2;
	}


	/**
	 * @return the quasar, if set
	 */
	public Quasar getQuasar() {
		return quasar;
	}


	/**
	 * @param quasar this emission line's quasar
	 */
	public void setQuasar(Quasar quasar) {
		this.quasar = quasar;
	}

		
}
