package jfilesyslib.utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A file system which may be written to or read from at the same time.<br>
 * The memory consists of chunks.<br>
 * It is <i>thread safe</i>.
 * @author Marc Miltenberger
 */
public class MemoryStream {
	public static final int DEFAULTCHUNKSIZE = 4096 * 4;
	private int CHUNKSIZE = DEFAULTCHUNKSIZE;
	
	/**
	 * Represents a single chunk
	 * @author Marc Miltenberger
	 */
	public class Chunk
	{
		byte[] chunk = new byte[CHUNKSIZE];
		int filled;
		int index;
	}
	private List<Chunk> chunks = new ArrayList<Chunk>();
	private Chunk currentChunk;
	private int positionWithinChunk = 0;
	private Chunk lastChunk;

	/**
	 * Creates a new instance of MemoryStream.
	 */
	public MemoryStream()  {
		currentChunk = new Chunk();
		lastChunk = currentChunk;
		chunks.add(currentChunk);
	}


	/**
	 * Creates a new instance of MemoryStream.
	 * @param chunksize the size of each chunk
	 */
	public MemoryStream(int chunksize)  {
		this.CHUNKSIZE = chunksize;
		currentChunk = new Chunk();
		lastChunk = currentChunk;
		chunks.add(currentChunk);
	}

	/**
	 * Creates a new instance of MemoryStream.
	 * @param memory the initial content
	 * @throws IOException 
	 */
	public MemoryStream(byte[] memory) throws IOException  {
		write(memory);
		seek(0);
	}

	/**
	 * Creates a new instance of MemoryStream.
	 * @param memory the initial content
	 * @param chunksize the size of each chunk
	 * @throws IOException 
	 */
	public MemoryStream(byte[] memory, int chunksize) throws IOException  {
		this.CHUNKSIZE = chunksize;
		currentChunk = new Chunk();
		lastChunk = currentChunk;
		chunks.add(currentChunk);
		write(memory);
		seek(0);
	}

	/**
	 * Creates a new instance of MemoryStream.
	 * @param chunkList the chunk list
	 */
	public MemoryStream(List<Chunk> chunkList) {
		this.chunks = chunkList;
		if (chunkList.size() == 0)
		{
			currentChunk = new Chunk();
			lastChunk = currentChunk;
			chunks.add(currentChunk);
		}
		if (chunkList.size() > 0)
		{
			this.lastChunk = chunkList.get(chunkList.size() - 1);
			this.currentChunk = chunkList.get(0);
			CHUNKSIZE = currentChunk.chunk.length;
		}
	}
	
	/**
	 * Flushes the stream (no effect)
	 */
	public void flush() {
		
	}
	

	/**
	 * Returns the content's length
	 * @return the content's length
	 */
	public int getLength() {
		if (chunks.size() == 0)
			return 0;
		return CHUNKSIZE * (chunks.size() - 1) + lastChunk.filled;
	}
	
	/**
	 * Returns the position
	 * @return the position
	 */
	public int getPosition()
	{
		return (currentChunk.index - 1) * CHUNKSIZE + currentChunk.filled;
	}
	
	/**
	 * Sets the new length
	 * @param newLength the new length
	 * @throws IOException
	 */
	public void setLength(long newLength) throws IOException {
		//int oldPos = getPosition();
		synchronized (chunks)
		{
			seek(newLength, true);
			currentChunk.filled = positionWithinChunk;
			for (int i = currentChunk.filled; i < CHUNKSIZE; i++)
				currentChunk.chunk[i] = 0;
			for (int i = chunks.size() - 1; i > currentChunk.index; i--)
				chunks.remove(i);
			lastChunk = currentChunk;
		}
	}

	/**
	 * Seeks to the specified offset. If the offset is larger than the content size, the content will <i>NOT</i> be streched.
	 * @param offset the offset
	 * @throws IOException
	 */
	public void seek(long offset) throws IOException {
		synchronized (chunks)
		{
			seek(offset, false);
		}
	} 

	/**
	 * Seeks to the specified offset. If the offset is larger than the content size, the content will be streched iff <i>stretch</i> is true.
	 * @param offset the offset
	 * @param stretch whether to stretch the content
	 * @throws IOException
	 */
	public void seek(long offset, boolean stretch) throws IOException {
		synchronized (chunks)
		{
			int chunkIndex = (int) (offset / CHUNKSIZE);
			if (stretch)
			{
				while (chunkIndex > chunks.size())
				{
					Chunk empty = new Chunk();
					empty.index = chunks.size();
					chunks.add(empty);
					lastChunk = empty;
				}
				for (int i = 0; i < chunkIndex; i++)
				{
					chunks.get(i).filled = CHUNKSIZE;
				}
			} else {
				long length = getLength();
				if (offset > length)
					throw new IllegalArgumentException("Offset out of range: offset = " + offset + ", length = " + length);

			}
			//At the end of the last chunk...
			if (chunkIndex > chunks.size() - 1)
			{
				Chunk empty = new Chunk();
				empty.index = chunks.size();
				empty.filled = 0;
				chunks.add(empty);
				lastChunk = empty;
			}
				
			currentChunk = chunks.get(chunkIndex);
			positionWithinChunk = (int) (offset - chunkIndex * CHUNKSIZE);
			if (stretch)
			{
				if (positionWithinChunk > currentChunk.filled)
					currentChunk.filled = positionWithinChunk;
			}
		}
	}

	/**
	 * Writes <i>input</i> to the memory stream.
	 * @param input the input data
	 * @throws IOException
	 */
	public void write(byte[] input) throws IOException {
		synchronized (chunks)
		{
			if (currentChunk == null)
			{
				currentChunk = new Chunk();
				chunks.add(currentChunk);
				lastChunk = currentChunk;
			}
			int toWrite = input.length;
			int posArray = 0;
			while (true)
			{
				if (positionWithinChunk + toWrite <= CHUNKSIZE)
				{
					System.arraycopy(input, posArray, currentChunk.chunk, positionWithinChunk, toWrite);
					positionWithinChunk += toWrite;
					int n = positionWithinChunk;
					if (n > currentChunk.filled)
						currentChunk.filled = n;
					if (isAtEnd() && positionWithinChunk == CHUNKSIZE)
					{
						Chunk newChunk = new Chunk();
						newChunk.index = currentChunk.index + 1;
						chunks.add(newChunk);
						lastChunk = newChunk;
						currentChunk = newChunk;
					}
					return;
				} else {
					int currentStep = CHUNKSIZE - positionWithinChunk;
					System.arraycopy(input, posArray, currentChunk.chunk, positionWithinChunk, currentStep);
					positionWithinChunk += currentStep;
					posArray += currentStep;
					toWrite -= currentStep;
					currentChunk.filled = CHUNKSIZE;
					
					if (isAtEnd())
					{
						Chunk newChunk = new Chunk();
						newChunk.index = currentChunk.index + 1;
						chunks.add(newChunk);
						lastChunk = newChunk;
						currentChunk = newChunk;
					} else {
						currentChunk = getNextChunk(currentChunk);
					}
					positionWithinChunk = 0;
				}
			}
		}
	}

	private boolean isAtEnd() {
		if (currentChunk != lastChunk)
			return false;
		if (positionWithinChunk == lastChunk.filled)
			return true;
		return false;
	}

	private Chunk getNextChunk(Chunk chunk) {
		return chunks.get(chunk.index + 1); 
	}

	/**
	 * Closes the memory stream.<br>
	 * It has no effect.
	 */
	public void close() {
		
	}

	/**
	 * Reads as much as possible into <i>content</i>
	 * @param content the output byte array
	 * @return the number of read bytes
	 */
	public int read(byte[] content) throws IOException {
		synchronized (chunks)
		{
			
			if (currentChunk == null)
				return 0;
			
			if (isAtEnd())
				return 0;
			
			int toRead = content.length;
			int posArray = 0;
			while (true)
			{
				if (positionWithinChunk + toRead <= currentChunk.filled)
				{
					System.arraycopy(currentChunk.chunk, positionWithinChunk, content, posArray, toRead);
					positionWithinChunk += toRead;
					return content.length;
				} else {
					int currentStep = currentChunk.filled - positionWithinChunk;
					try 
					{
						System.arraycopy(currentChunk.chunk, positionWithinChunk, content, posArray, currentStep);
					} catch (Exception ex)
					{
						ex.printStackTrace();
					}
					posArray += currentStep;
					toRead -= currentStep;
					positionWithinChunk += currentStep;
					boolean isAtEnd = isAtEnd();
					
					if (isAtEnd)
						return content.length - toRead;
					
					currentChunk = getNextChunk(currentChunk);
					positionWithinChunk = 0;
				}
			}
		}
	}


	/**
	 * Returns the used chunk list
	 * @return the used chunk list
	 */
	public List<Chunk> getChunkList() {
		return chunks;
	}
	
	/**
	 * Returns the content as a single byte array
	 * @return the content as a single byte array
	 * @throws IOException
	 */
	public byte[] toArray() throws IOException
	{
		int pos = getPosition();
		byte[] res = new byte[getLength()];
		seek(0);
		read(res);
		seek(pos);
		return res;
	}
}
 