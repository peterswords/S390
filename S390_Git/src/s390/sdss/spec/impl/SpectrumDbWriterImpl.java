/*
 * (C) COPYRIGHT Peter Swords, 2014. All rights reserved.
 * This code is provided for informational purposes only.
 * No right to use is granted, and no warranty as to 
 * fitness for any purpose is hereby expressed or implied.
 */
package s390.sdss.spec.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import ps.util.SimpleMapEntry;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class SpectrumDbWriterImpl {

	/**
	 * An index entry consists of a Long unique object id and a Long offset
	 * within the spectrum index file. The total length of an index entry is
	 * therefore 2 * 8 = 16 bytes.
	 */
	private final static int kIndexEntrySize = 16;

	
	/**
	 * Index is represented by a list of pairs of unique object ids and offsets
	 * to the corresponding object in the spectrum database file.
	 */
	List<SimpleMapEntry<Long, Long>> indexList = new ArrayList<SimpleMapEntry<Long, Long>>();

	
	/**
	 * Channel to output database file
	 */
	SeekableByteChannel chan;

	
	/**
	 * Indicate if "lite" database writer.
	 */
	private boolean lite;

	
	/**
	 * Constructor: create a new output database file.
	 * @param path path to database file.
	 * @param lite indicates if we are to write the smaller "lite" version of the database.
	 * @throws IOException if the file cannot be opened or already exists.
	 */
	public SpectrumDbWriterImpl(Path path, boolean lite) throws IOException {
		
		this.lite = lite;
		
		chan = Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		
		// The first long in the file is the offset of the database index. We can't write the index
		// until we have written all the objects, so we write a placeholder zero which we will
		// overwrite later.
		writeLong(0L);
		
		// the second long indicates if lite version in use
		writeLong(lite? -1L : 1L);
		
	}
	
	
	/**
	 * Add a spectrum to the database.
	 * @param spec the spectrum to write
	 * @throws IOException if an I/O error occurs.
	 */
	public void add(SpectrumImpl spec) throws IOException {

		// Add an entry to the index. Since the channel is currently
		// positioned where the object will be written, we can use this
		// position as the offset for the index entry.
		indexList.add(new SimpleMapEntry<>(spec.getObjID(), chan.position()));
		
		// Put spectrum in buffer
		ByteBuffer buf = spec.toByteBuffer(lite);
		
		// The length of the spectrum buffer is its current position after creation.
		// Write this length as the first long in the object in the database
		writeLong(buf.position());
		
		// Then write the buffer
		writeOut(buf);
	}

	
	/**
	 * Write out a buffer from beginning to its current position. After writing,
	 * the buffer is positioned at its beginning.
	 * 
	 * @param b
	 *            the buffer
	 * @throws IOException
	 */
	private void writeOut(ByteBuffer b) throws IOException {
		b.limit(b.position());
		b.position(0);
		chan.write(b);
	}
	
	
	/**
	 * When finished writing all spectra, write the index.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void finish() throws IOException {

		// After writing the last spectrum, the file is positioned
		// where the index will start, which we note.
		long indexOffset = chan.position();
		
		// Allocate a buffer to hold the index. Add 1Kb of slop to size.
		ByteBuffer buf = ByteBuffer.allocate(indexList.size() * kIndexEntrySize
				+ 1024);
		
		// First int in index is the number of index entries
		buf.putInt(indexList.size());
		
		// Put the index entries in the buffer. Each one consists of long object
		// id and long file offset of object
		for (int i = 0; i < indexList.size(); i++) {
			buf.putLong(indexList.get(i).getKey());
			buf.putLong(indexList.get(i).getValue());
		}
		
		// Write the size of the index buffer to the file as a long
		writeLong(buf.position());
		
		// Then write the index buffer itself
		writeOut(buf);
		
		// Now reposition to the start of the file where we wrote
		// a placeholder to hold the offset of the start of the index.
		chan.position(0);
		
		// Overwrite with actual start of index which we noted at top
		// of this method
		writeLong(indexOffset);
		
		// Close the database file. We are done!
		chan.close();
		
	}
	
	
	/**
	 * Write a long integer value to the file.
	 * @param l long to write
	 * @throws IOException
	 */
	private void writeLong(long l) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(8);
		b.putLong(l);
		writeOut(b);
	}
	
}
