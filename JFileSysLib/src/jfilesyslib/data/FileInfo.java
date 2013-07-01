package jfilesyslib.data;

/**
 * This class should be used to model files
 * @author Marc Miltenberger
 */
public class FileInfo extends EntityInfo {
	private long fileSize;

	/**
	 * Creates a new FileInfo object.
	 * @param fullPath the full path to the file. e.g.: "/Dir/file.dat"
	 * @param fileSize the file size in bytes
	 */
	public FileInfo(String fullPath, long fileSize)
	{
		this.setFullPath(fullPath);
		this.setFileSize(fileSize);
	}

	/**
	 * Returns the file size in bytes
	 * @return the file size in bytes
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * Sets the file size in bytes
	 * @param fileSize the file size
	 */
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
}
