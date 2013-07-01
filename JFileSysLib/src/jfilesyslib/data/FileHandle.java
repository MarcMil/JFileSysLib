package jfilesyslib.data;

/**
 * Represents a file handle.
 * 
 * It saves an application specific object, which may be basically everything.
 * @author Marc Miltenberger
 */
public class FileHandle {
	private String filePath;
	private Object objHandle;
	/**
	 * Whether the file was empty<br>
	 * Should not be used by your file system.
	 */
	public boolean isEmptyFile;
	
	/**
	 * Whether reading is allowed.<br>
	 * Should not be used by your file system.
	 */
	public boolean read;
	
	/**
	 * Whether writing is allowed.<br>
	 * Should not be used by your file system.
	 */
	public boolean write;
	
	/**
	 * Whether the file handle has been closed.<br>
	 * Should not be used by your file system.
	 */
	public boolean hasClosed = false;
	
	/**
	 * Creates a new FileHandle object
	 * @param filePath the file path
	 */
	public FileHandle(String filePath) {
		this.filePath = filePath;
	}
	
	/**
	 * Creates a new FileHandle object
	 * @param filePath the file path
	 * @param objHandle the handle used by the application
	 */
	public FileHandle(String filePath, Object objHandle) {
		this.filePath = filePath;
		this.objHandle = objHandle;
	}
	
	/**
	 * Returns the file path of the handle
	 * @return the file path of the handle
	 */
	public String getFilePath()
	{
		return filePath;
	}

	/**
	 * Returns the application specific handle
	 * @return the application specific handle
	 */
	public Object getObjHandle() {
		return objHandle;
	}

	/**
	 * Sets the application specific handle
	 * @param objHandle the application specific handle
	 */
	public void setObjHandle(Object objHandle) {
		this.objHandle = objHandle;
	}
}
