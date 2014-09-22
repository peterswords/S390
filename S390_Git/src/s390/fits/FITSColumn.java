/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.fits;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Specification for special conversion of FITS table column to SQL database
 * column.
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public interface FITSColumn {

	/**
	 * Get the column name. This will be matched against FITS column names to
	 * find the column for special treatment.
	 * 
	 * @return the column name
	 */
	String getColName();

	
	/**
	 * Convert the FITS column value to its SQL value. The default
	 * implementation just returns the original FITS value.
	 * 
	 * @param origValue
	 *            the FITS-format value
	 * @return the convered value
	 */
	default Object convert(Object origValue) {
		
		return origValue;
		
	}

	
	/**
	 * Accept a row. If the row is not accepted it is not passed to any Consumer
	 * or written to the database. For a row to be accepted it has to be
	 * accepted by all columns. If any column rejects it, it is discarded. The
	 * value passed to the acceptRow method is the original value from the FITS
	 * binary table, not the converted value returned by convert(Object). Array
	 * columns will have array types. The default is to accept all rows.
	 * 
	 * @param origValue
	 *            the FITS-format value
	 * @return true to accept the row or false to discard it.
	 */
	default boolean acceptRow(Object origValue) {

		return true;

	}
	
	
	/**
	 * Accept an iteration of a column. For FITS array columns it may not be
	 * required to convert all the instances. By accepting an iteration it will
	 * be converted, otherwise it does not become a column on the target table.
	 * Non-array columns will have acceptIteration called once with a value of
	 * 1, array columns are called for 1 to n. The default is to accept all
	 * columns.
	 * 
	 * @param iter
	 *            the iteration number, starting at 1.
	 * @return true to accept the column or false to omit it.
	 */
	default boolean acceptIteration(int iter) {
		
		return true;
		
	}

	
	/**
	 * Optionally replace the column SQL definition. This should not include the
	 * column name, which cannot be changed. The input might be, for instance a
	 * numeric string which we want to convert to a numeric type and make it a
	 * key. Thus, the input might be "varchar(4)" while the output might be
	 * "smallint primary key". The default implementation returns the input
	 * definition unchanged.
	 * 
	 * @param colDef
	 *            the input definition
	 * @return the output modified definition
	 */
	 default String getColDefReplace(String colDef) {
		
		return colDef;

	}

	
	/**
	 * A type of column override that allows column name and override type to be specified via constructor.
	 * @author peter
	 *
	 */
	public static class Named implements FITSColumn {
		
		String colName;

		
		public Named(String colName) {
			
			this.colName = colName;
			
		}
	
		
		@Override
		public String getColName() {
			
			return colName;
			
		}
		
	}
	

	/**
	 * A type of column override that allows column name and override type to be specified via constructor.
	 * @author peter
	 *
	 */
	public static class Type extends Named {
		
		String colType;

		
		public Type(String colName, String colType) {
			
			super(colName);
			this.colType = colType;
			
		}
		
		
		@Override
		public String getColDefReplace(String colDef) {
			
			return colType;
			
		}
		
	}
	

	/**
	 * A type of column override that allows column name and accepted iterations
	 * to be specified via constructor.
	 * 
	 * @author peter
	 *
	 */
	public static class Iter extends Named {
		
		Set<Integer> iters;

		
		public Iter(String colName, int... acceptedCol) {
			
			super(colName);
			iters = IntStream.of(acceptedCol).mapToObj(i -> i)
					.collect(Collectors.toSet());
			
		}
		
		@Override
		public boolean acceptIteration(int iter) {
			
			return iters.contains(iter);
			
		}
		
	}
	

	/**
	 * A type of column override that provides a settable ID column.
	 * @author peter
	 *
	 */
	public static class ID extends Type {
		
		protected long idCol;

		
		public ID (String colName, String colType) {
			
			super(colName, colType);
			
		}

		
		@Override
		public Object convert(Object origValue) {
			
			return idCol;

		}
		
		public void setID(long id) {
			
			idCol = id;
			
		}
		
	}
	

	/**
	 * A type of column override that provides an automatically incrementing counter column.
	 * @author peter
	 *
	 */
	public static class Counter extends ID {
		
		public Counter(String colName, String colType) {
			
			super(colName, colType);
			
		}		

		
		@Override
		public Object convert(Object origValue) {

			return idCol++;
			
		}

		
		public void reset() {

			setID(0);
			
		}
		
	}
	
}
