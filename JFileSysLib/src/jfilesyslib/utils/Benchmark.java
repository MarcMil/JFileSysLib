package jfilesyslib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Performs some simple benchmarks
 * @author Marc Miltenberger
 */
public class Benchmark {
	/**
	 * Represents a benchmark result
	 * @author Marc Miltenberger
	 */
	public static class BenchmarkResult
	{
		/**
		 * How long the write process took (in milliseconds)
		 */
		public int writeMs;

		/**
		 * How long the read process took (in milliseconds)
		 */
		public int readMs;
		
		/**
		 * The written kilobytes 
		 */
		public int kib;
		
		/**
		 * The used block size 
		 */
		public int blockSize;
		
		private static final String NEWLINE = "\n";

		@Override
		public String toString() {
			return "Block size: " + blockSize + NEWLINE + "Write (ms): " + writeMs + NEWLINE + "Read (ms): " + readMs + NEWLINE + "KiB: " + kib;
		}
	}
	
	private Benchmark() {
	}
	
	/**
	 * Performs a benchmark on the specified file. It uses various block sizes.<br>
	 * <b>Warning:</b> the file will be <b>overwritten</b>!
	 * @param file the file
	 * @return the benchmark result
	 * @throws IOException
	 */
	public static BenchmarkResult[] runBenchmark(File file) throws IOException
	{
		int[] blocksizes = new int[] { 512, 1024, 2048, 4096, 8192, 16384, 32768 };
		int c = 400;
		int[] counts = new int[] { c * 64, c * 32, c * 16, c * 8, c* 4, c * 2, c * 1 };
		BenchmarkResult[] res = new BenchmarkResult[counts.length];
		for (int i = 0; i < res.length; i++)
		{
			res[i] = runBenchmark(file, blocksizes[i], counts[i]);
			/*System.out.println(res[i]);
			System.out.println(""); */
		}
		file.delete();
		return res;
	}

	/**
	 * Performs a benchmark on the specified file.<br>
	 * <b>Warning:</b> the file will be <b>overwritten</b>!
	 * @param file the file
	 * @param blocksize the block size to use
	 * @param count the number of bytes to write
	 * @return the benchmark result
	 * @throws IOException
	 */
	public static BenchmarkResult runBenchmark(File file, int blocksize, int count) throws IOException
	{
		BenchmarkResult res = new BenchmarkResult();
		res.blockSize = blocksize;
		byte[] buffer = new byte[blocksize];
		//Random rand = new Random();
		//rand.nextBytes(buffer);
		FileOutputStream stream = new FileOutputStream(file);
		int kib = (buffer.length * count) / (1024);
		res.kib = kib;
		long startWrite = System.currentTimeMillis();
		for (int i = 0; i < count; i++)
			stream.write(buffer, 0, buffer.length);
		stream.flush();
		stream.close();
		long endWrite = System.currentTimeMillis();
		res.writeMs = (int)(endWrite - startWrite);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		FileInputStream instream = new FileInputStream(file);
		long startRead = System.currentTimeMillis();
		for (int i = 0; i < count; i++)
			instream.read(buffer, 0, buffer.length);
		long endRead = System.currentTimeMillis();
		res.readMs = (int)(endRead - startRead);
		try {
			instream.close();
		} catch (Exception e) {}
		return res;
	}
	
}
