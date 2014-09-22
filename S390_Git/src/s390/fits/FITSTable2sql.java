/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.fits;

import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Consumer;

import ps.db.SQL;
import ps.util.TextUtils;

/**
 * A helper class which interrogates FITS table metadata, and helps with loading
 * binary tables onto an SQL database. This class maps from FITS datatypes to
 * SQL data types, creates a relational table definition corresponding to a FITS
 * binary table, creates an appropriate SQL insert statement, and transfers data
 * from the FITS binary table to the database table. Optionally, instead of
 * updating a relational database, clients can provide a data consumer to
 * perform any processing they want with the supplied data. In either case,
 * clients can optionally specify the subset of table columns they want, and --
 * using "column overrides" -- any special conversions they want to perform.
 * 
 * @see <a href="http://fits.gsfc.nasa.gov/">NASA FITS documentation</a>
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class FITSTable2sql {

	/**
	 * FITS table being processed
	 */
	private FITSTable table;
	
	
	/**
	 * The relational table name passed to our constructor
	 */
	private String strTableName;

	
	/**
	 * The relational table definition created by our constructor
	 */
	private String strTableDef;

	
	/**
	 * A useful SQL insert statement for the table created by our constructor
	 */
	private String strInsertStatement;

	
	/**
	 * We optionally only want to deal with a subset of columns from the table.
	 * This array stores the column indexes of those columns we want.
	 */
	private int colIndex[];

	
	/**
	 * A useful comma-separated list of output column names created by our
	 * constructor
	 */
	private String strColNames;

	
	/**
	 * Most columns exist only once, but in a FITS binary table there can be
	 * array type columns with repeated iterations. Here we store the number of
	 * iterations for each column.
	 */
	private int colIterations[];

	
	/**
	 * An array of optional column converters which allows for special handling
	 * of certain columns. To simplify the processing, any column for which
	 * there is no special conversion is given an "identity" conversion.
	 */
	private FITSColumn colConvert[];

	
	/**
	 * Create an "identity converter" which does nothing. This is used for any
	 * column that has no other special conversion
	 * 
	 */
	private static FITSColumn kIdentityConvert = new FITSColumn() {
		@Override
		public String getColName() {
			return null;
		}
	};
	
	/**
	 * The total number of columns including each accepted iteration of array columns.
	 */
	private int nTotalCols;

	
	/**
	 * Constructor.
	 * 
	 * @param tableName
	 *            the name of the database table that will be created/updated
	 * @param table
	 *            the FITS table containing the data that will be loaded
	 */
	public FITSTable2sql(String tableName, FITSTable table) {

		this(tableName, table, null, null);
		
	}

	
	/**
	 * Constructor.
	 * 
	 * @param tableName
	 *            the name of the database table that will be created/updated
	 * @param tbl
	 *            the FITS table containing the data that will be loaded
	 * @param colNames
	 *            optional list of column names of the subset of columns we
	 *            want. A null or empty list means use all columns.
	 * @param converters
	 *            a list of optional column overrides for special processing
	 */
	public FITSTable2sql(String tableName, FITSTable tbl, List<String> colNames, List<FITSColumn> converters) {

		table = tbl;
		strTableName = tableName;
		
		// Get column metadata from FITS table -- names, types,units,comments
		List<String> allColNames = table.getColumnNames();
		List<String> allColTypes = table.getColumnTypes();
		List<String> allColUnits = table.getColumnUnits();
		List<String> allColComments = table.getColumnComments();

		// Compute some other metadata
		collectMetadata(colNames, converters, allColNames);
		
		/*
		 * Construct list of column names and column definitions, which will be
		 * used as part of table creation sql and insert statement sql later.
		 * The values list is just a list of question marks used as argument
		 * placeholders in the insert statement.
		 */

		StringBuilder sbColNames = new StringBuilder();
		StringBuilder sbColDefs = new StringBuilder();
		StringBuilder sbValuesList = new StringBuilder();
		
		int numNamedCols = 0;
		// there can be more total cols than named ones, since array cols can
		// have multiple iterations
		nTotalCols = 0;
		for (int index : colIndex) {
			
			/*
			 * It's possible for a column to be named on the provided column
			 * list that doesn't appear at all in the FITS table. This allows
			 * for creation of new columns in the output that don't exist in the
			 * input. In that case we *must* have a converter that provides
			 * values for the new column.
			 */
			String colName = (index == -1)? colNames.get(numNamedCols) : allColNames.get(index);
			if ((index == -1) && (colConvert[numNamedCols] == kIdentityConvert)) {
				throw new RuntimeException("Column not in FITS table and no special converter specified: " +  colName);
			}
			
			// Create the column definition, which may be overriden by special converter
			String colDef = index == -1 ? null : getColType(allColTypes.get(index))
					+ getComment(allColComments.get(index), allColUnits.get(index));
			
			// Get number of column iterations for array columns
			int colIter = index == -1 ? 1 : getColCount(allColTypes.get(index));
			colIterations[numNamedCols] = colIter;

			// Loop for all iterations of an array column. Since most columns are not
			// arrays we mostly just execute this loop once for plain columns
			for (int colIterIndex = 1; colIterIndex <= colIter; colIterIndex++) {
				
				// See if the converter "accepts" this iteration.
				if (!colConvert[numNamedCols].acceptIteration(colIterIndex)) {
					// Converter doesn't want it, skip this column iteration
					continue;
				}
				
				// Commas in various lists
				if (nTotalCols > 0) {
					// Separator commas etc.
					sbColDefs.append(",\n");
					sbColNames.append(',');
					sbValuesList.append(',');
				}
				
				// Append col number to names of array columns
				String colNum = (colIter > 1)? String.valueOf(colIterIndex) : "";
				sbColDefs.append(colName + colNum);
				
				// Add column to column name and definition lists
				sbColNames.append(colName + colNum);
				sbColDefs.append(' ');
				sbColDefs.append(colConvert[numNamedCols].getColDefReplace(colDef));
				
				// Add a placeholder to the values list of insert statement
				sbValuesList.append('?');
				
				nTotalCols++;
			}
			
			numNamedCols++;
		}
		
		// Finish off sql strings
		strTableDef = String.format("create table %s (\n%s)", tableName, sbColDefs.toString());
		strColNames = sbColNames.toString();
		strInsertStatement = String.format("insert into %s (%s) values (%s)",
				tableName, strColNames, sbValuesList.toString());
		
	}

	
	private void collectMetadata(List<String> colNames,
			List<FITSColumn> converters, List<String> allColNames) {		
		
		// Get indexes of our subset of column names in the total list.
		// If no subset is provided, use the full set.
		if ((colNames == null) || colNames.isEmpty()) {
			colNames = allColNames;
		}
		
		// Note nCols is the size of our subset, NOT the whole set of table columns
		int nCols = colNames.size();
		
		// Create arrays to hold metadata about our subset of columns
		colIndex = new int[nCols];
		colIterations = new int[nCols];
		colConvert = new FITSColumn[nCols];
		
		// Iterate over our selected subset of columns
		for (int i = 0; i < nCols; i++) {
			
			// Get index into list of all columns
			colIndex[i] = allColNames.indexOf(colNames.get(i));
			
			// default to identity conversion
			colConvert[i] = kIdentityConvert;
			
			// Now search for specific conversion overrides
			if (converters != null) {

				for (FITSColumn cnv : converters) {
					if (colNames.get(i).equals(cnv.getColName())) {
						colConvert[i] = cnv;
						break;
					}
				}
				
			}
			
		}
		
	}
	
		
	/**
	 * Inserts FITS table data onto a relational database. Data is inserted from
	 * the same table that was passed to the constructor.
	 * 
	 * @param sql
	 *            SQL object for manipulating database
	 * 
	 */
	public void loadSQLData(SQL sql) {
		
		// Consume the data by passing each row to an SQL insert statement
		loadData( data -> sql.update(strInsertStatement, data));
		
	}

	
	/**
	 * Consume FITS table data. Data is provided from
	 * the same table that was passed to the constructor.
	 * 
	 * @param dataConsumer a consumer for the data, called for each row.
	 * 
	 */
	public void loadData(Consumer<Object[]> dataConsumer) {
		
		// Allocate storage for the columns of the input FITS tables
		// and populate it. Note that FITS gives us the data in the
		// form of entire columns.
		int nCols = colIndex.length;
		Object[] colData = new Object[nCols];
		
		for (int i = 0; i < nCols; i++) {
			int index = colIndex[i];
			colData[i] = index == -1? null : table.getColumn(index);
		}
		
		// Allocate storage to hold one row of data
		Object dbdata[] = new Object[nTotalCols];
		
		// Iterate to convert data from row format to column format
		for (int row = 0; row < table.getNRows(); row++) {
			
			int totalCol = 0;
			
			boolean rowAccepted = true;
			
			for (int col = 0; col < nCols; col++) {
				Object value = colData[col] == null? null : Array.get(colData[col], row); 
				
				rowAccepted = rowAccepted && colConvert[col].acceptRow(value);
				
				if (colIterations[col] == 1) {
					
					// Convert simple column
					if (colConvert[col].acceptIteration(1)) {
						dbdata[totalCol] = colConvert[col].convert(value);
						totalCol++;
					}
					
				} else {
					
					// Convert array column
					for (int i = 0; i < colIterations[col]; i++) {
						
						if (colConvert[col].acceptIteration(i + 1)) {
							Object arrValue = (value == null)? null : Array.get(value, i);
							dbdata[totalCol] = colConvert[col].convert(arrValue);
							totalCol++;
						}
						
					}
					
				}
				
			}
			
			// Consume the current row
			if (rowAccepted) {
				dataConsumer.accept(dbdata);
			}
		
		}
		
	}

	
	/**
	 * Inserts FITS table data onto a relational database. Data is inserted from
	 * 'anotherTable', not necessarily the one that was passed to the
	 * constructor. The table must have the same format as the one passed to the
	 * constructor or the results are unpredictable.
	 * 
	 * @param sql
	 *            SQL object for manipulating database
	 * @param anotherTable the table from which data is to be inserted.
	 */
	public void loadSQLData(SQL sql, FITSTable anotherTable) {

		this.table = anotherTable;
		loadSQLData(sql);

	}

	
	/**
	 * Consume FITS table data. Data is provided from
	 * 'anotherTable', not necessarily the one that was passed to the
	 * constructor. The table must have the same format as the one passed to the
	 * constructor or the results are unpredictable.
	 * 
	 * @param dataConsumer a consumer for the data, called for each row.
	 * @param anotherTable the table from which data is to be inserted.
	 */
	public void loadData(Consumer<Object[]> dataConsumer, FITSTable anotherTable) {

		this.table = anotherTable;
		loadData(dataConsumer);

	}

	
	/**
	 * Get the SQL table name.
	 * @return the SQL table name.
	 */
	public String getTableName() {

		return strTableName;

	}

	
	/**
	 * Get the SQL table definition corresponding to this table.
	 * @return the SQL table creation string.
	 */
	public String getTableDef() {

		return strTableDef;

	}

	
	/**
	 * Get the SQL insert statement corresponding to this table.
	 * @return the SQL insert string.
	 */
	public String getInsertStmt() {
		
		return strInsertStatement;

	}
	
	
	/**
	 * Get the comma-separated list of column names
	 * @return the csv column name string.
	 */
	public String getColumnNames() {
		
		return strColNames;
		
	}

	
	/**
	 * Helper method which string together a comment for the SQL table
	 * creation from the comment and units metadata in the FITS file.
	 * @param comment comment string
	 * @param unit units string, optionally blank
	 * @return formatted comment string for SQL
	 */
	private String getComment(String comment, String unit) {
		
		if (TextUtils.nullOrEmpty(unit)) {
			unit = "";
		} else {
			unit = unit.equals("-")? "" : "; unit: " + unit;
		}
		
		if (TextUtils.nullOrEmpty(comment) && TextUtils.nullOrEmpty(unit)) {
			return "";
		}
		
		return " comment '" + comment + unit +"'";
		
	}


	/**
	 * Helper method to convert a FITS datatype string into an
	 * SQL datatype string.
	 * @param fitsType FITS type
	 * @return the column type as an SQL datatype
	 */
	private String getColType(String fitsType) {
		
		char ch = fitsType.charAt(fitsType.length() - 1);
		
		switch (ch) {
			case 'I': return "smallint";
			case 'J': return "int";
			case 'K': return "bigint";
			case 'E': return "real";
			case 'D': return "double";
			case 'A': return (fitsType.length() == 1)? "varchar(1)" :
				"varchar(" + fitsType.substring(0, fitsType.length() - 1) +")";
			default: throw new RuntimeException("Unknown FITS column type " + fitsType);
		}
		
	}


	/**
	 * Helper method to convert a FITS datatype string into an
	 * number of iterations (FITS columns can themselves contain many rows).
	 * Note that text columns are always treated as a single iteration even
	 * though they can have many characters.
	 * @param fitsType FITS type
	 * @return count of column iterations. Always 1 text.
	 */
	private int getColCount(String fitsType) {
		
		if ((fitsType.length() == 1) || fitsType.endsWith("A")) {
			return 1;
		}
		
		fitsType = fitsType.substring(0, fitsType.length() - 1);
		return Integer.valueOf(fitsType);
		
	}

}
