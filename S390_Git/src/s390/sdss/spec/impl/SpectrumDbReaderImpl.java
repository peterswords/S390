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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ps.util.KeyValuePair;
import s390.sdss.spec.Spectrum;
import s390.sdss.spec.SpectrumDbReader;

/**
 * 
 * @author Peter Swords email s3923-ou@yahoo.ie
 *
 */
public class SpectrumDbReaderImpl implements SpectrumDbReader {

	/**
	 * Flag indicating if we should use a buffer for reading Spectrum db.
	 * Otherwise we read exactly one spectrum in at a time.
	 */
	private static final boolean useBuffering = true;

	
	/**
	 * Default buffer size if {@link #useBuffering} is true.
	 */
	private static final int kBufferDefaultSize = 1 << 23; // 8 Mb
	
	
	/**
	 * The buffer used if {@link #useBuffering} is true.
	 */
	private ByteBuffer currentBuffer;
	

	/**
	 * Offset of start of current buffer into the file
	 */
	private long currentBufferStart = Long.MAX_VALUE;

	
	/**
	 * Index is represented by a list of pairs of unique object ids and offsets
	 * to the corresponding object in the spectrum database file.
	 */
	List<KeyValuePair<Long, Long>> indexList = new ArrayList<KeyValuePair<Long, Long>>();

	
	/**
	 * Random I/O channel to database file
	 */
	private SeekableByteChannel chan;


	/**
	 * Indicates if database is smaller "lite" version.
	 */
	private boolean lite;
	
	
	/**
	 * Constructor: open the database file for reading.
	 * @param path path of database file
	 * @throws IOException if the file is not found
	 */
	public SpectrumDbReaderImpl(Path path) throws IOException {
		if (useBuffering) {
			currentBuffer = ByteBuffer.allocate(kBufferDefaultSize);
		}
		chan = Files.newByteChannel(path, StandardOpenOption.READ);
		loadIndex();
	}

	
	/**
	 * Load the index from the database file.
	 * @throws IOException
	 */
	private void loadIndex() throws IOException {
		
		// The first long in the file is the offset of the index.
		// Get the offset and position to the index.
		long indexOffset = readLong();
		long liteInd = readLong();
		this.lite = liteInd == -1;
		
		chan.position(indexOffset);
		
		// The first long in the index is the size in bytes of the rest of the index.
		// Allocate a buffer to hold the rest of the index, and read it in.
		long indexSize = readLong();
		ByteBuffer buf = ByteBuffer.allocate((int) indexSize);
		readIn(buf);
		
		// The first int in the index is the number of index entries
		int indexEntries = buf.getInt();
				
		// Each entry in the index is a pair of longs, containing a unique
		// object id and an offset into the file of the object. Get them all.
		indexList = new ArrayList<KeyValuePair<Long, Long>>(indexEntries);
		for (int i = 0; i < indexEntries; i++) {
			KeyValuePair<Long, Long> entry = new KeyValuePair<>(buf.getLong(), buf.getLong());
			indexList.add(entry);
		}
		
		// Sort index by object id
		Collections.sort(indexList, Map.Entry.comparingByKey());
	}

	/**
	 * Read data into a buffer starting at the beginning of the buffer,
	 * and ensuring that it is completely filled after the read.
	 * @param b the buffer
	 * @throws IOException
	 */
	private void readIn(ByteBuffer b) throws IOException {
		
		// Position to start of buffer
		b.position(0);
		
		// Read into buffer
		chan.read(b);
		
		// Make sure buffer is full
		if (b.position() != b.limit()) {
			throw new RuntimeException("Read underflow");
		}
		
		// Set buffer to start again
		b.position(0);
	}
	
	/**
	 * Read an eight byte long integer from the file
	 * @return long value from file
	 * @throws IOException
	 */
	private long readLong() throws IOException {
		ByteBuffer b = ByteBuffer.allocate(8);
		readIn(b);
		return b.getLong();
	}
	
	
	/**
	 * Return a stream of all the unique ids of all objects in the index.
	 * Entries are in ascending order.
	 */
	@Override
	public Stream<Long> idStream() {
		
		return StreamSupport.stream(indexList.spliterator(), false).map(p -> p.getKey());
		
	}
	
	
	/**
	 * Return all the unique ids of all objects in the index.
	 * Entries are in ascending order.
	 */
	@Override
	public Iterable<Long> getAllObjID() {
		
		return new Iterable<Long>() {

			@Override
			public Iterator<Long> iterator() {
				
				return new Iterator<Long>() {

					Iterator<KeyValuePair<Long, Long>> i = indexList.iterator();
					
					@Override
					public boolean hasNext() {
						
						return i.hasNext();
					}

					@Override
					public Long next() {
						
						return i.next().getKey();

					}
					
				};
			}
			
		};

	}

	
	/**
	 * Get a spectrum from the database.
	 */
	@Override
	public Spectrum get(long objID) {

		// Construct index key and search for it.
		KeyValuePair<Long, Long> key = new KeyValuePair<>(objID, null);
		int index = Collections.binarySearch(indexList, key, Map.Entry.comparingByKey());
		if (index < 0) {
			// not found
			return null;
		}
		
		// Retrieve the found index entry. The offset to the object in the
		// database file is in the second long of the key.
		key = indexList.get(index);
		long pos = key.getValue();
		
		try {
			
			// Type of read differs depending on whether we are using buffering
			ByteBuffer buf  = useBuffering? getFromBuffer(pos) : getFromChan(pos);
			
			// Construct spectrum from the buffer
			return SpectrumImpl.fromByteBuffer(buf, lite);
			
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}


	/**
	 * Get an object from the file. The length of the object is stored in the
	 * file at position pos, followed by the object itself. Before resorting to
	 * the file, we check whether the requried storage has already been
	 * buffered, and return a slice of that buffer if possible. Otherwise we
	 * fill another buffer.
	 * 
	 * @param pos
	 *            position of object to be read in
	 * @return a ByteBuffer containing the required object
	 * @throws IOException
	 */
	private ByteBuffer getFromBuffer(long pos) throws IOException {
		
		/*
		 * Check if the required position is in the currently loaded buffer. The
		 * start of the current buffer must be less than or equal to the
		 * required position, and the buffer must contain at least eight bytes
		 * beyond pos (so that we at least have a long containing the length of
		 * the requred object in the buffer.
		 */
		if ((currentBufferStart <= pos) && (currentBuffer.limit() > (pos - currentBufferStart + 8))) {
			// position to the start of the object in the buffer and read its length
			int positionInBuffer = (int)(pos - currentBufferStart);
			currentBuffer.position(positionInBuffer);
			long len = currentBuffer.getLong();
			/**
			 * We're not out of the woods yet ... equipped with the length, we now have to 
			 * make sure the required object is entirely within the buffer.
			 */
			if (currentBuffer.limit() - currentBuffer.position() >= len) {
				// We have the required object. Slice the buffer at this point
				// and return the slice.
				return currentBuffer.slice();
			}
		}
		
		/*
		 * If we get here, we didn't have the required object in the buffer so we will
		 * have to load up a new buffer from the file.
		 */
		
		// Move channel to position
		chan.position(pos);
		currentBufferStart = pos;
		
		// Position to start of buffer
		currentBuffer.position(0);
		
		// Read into buffer
		chan.read(currentBuffer);
		
		// Set buffer to start again
		currentBuffer.position(0);

		// Get the length of the object from the buffer.
		long len = currentBuffer.getLong();
		
		if (len <= (kBufferDefaultSize - currentBuffer.position())) {
			// we've got the full object in the buffer. Slice to current position.
			return currentBuffer.slice();
		} else {
			/*
			 * The default buffer should be sized bigger than any object we would ever
			 * want to read,  but just in case it isn't, we can resort to reading full
			 * objects straight from the file.
			 */
			return getFromChan(pos);
		}

	}


	/**
	 * Get an object from the file. The length of the object is stored in the
	 * file at position pos, followed by the object itself.
	 * 
	 * @param pos
	 *            position of object to be read in
	 * @return a ByteBuffer containing the required object
	 * @throws IOException
	 */
	private ByteBuffer getFromChan(long pos) throws IOException {
		
		// Position to the object
		chan.position(pos);
		
		// First long in object is object length
		long len = readLong();
		
		// Allocated buffer of exact required size for object and read it
		ByteBuffer buf = ByteBuffer.allocate((int)len);
		readIn(buf);
		return buf;
		
	}
	
}
