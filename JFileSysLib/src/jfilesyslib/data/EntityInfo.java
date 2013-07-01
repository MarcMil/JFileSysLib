package jfilesyslib.data;

/**
 * This is the common base class of Directories, Files and Symbolic Links
 * @author Marc Miltenberger
 */
public class EntityInfo {
	private long lastAccessTime;
	private long creationTime;
	private long lastModificationTime;
	private String fullPath;
	
	/**
	 * Returns only the file name.<br>
	 * E.g. if the full path = "/testDir/testfile.dat"<br>
	 * it returns "testfile.dat"
	 * @return the file name
	 */
	public String getFileName()
	{
		return getFullPath().substring(getFullPath().lastIndexOf('/') + 1);
	}

	/**
	 * Returns the full path to the file/directory/symlink
	 * @return the full path
	 */
	public String getFullPath() {
		if (fullPath.isEmpty())
			return "/";
		return fullPath;
	}

	/**
	 * Sets the full path
	 * @param fullPath the full path
	 */
	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	/**
	 * Returns the last modification time
	 * @return the last modification time
	 */
	public long getLastModificationTime() {
		return lastModificationTime;
	}

	/**
	 * Sets the last modification time
	 * @param lastModificationTime the last modification time
	 */
	public void setLastModificationTime(long lastModificationTime) {
		this.lastModificationTime = lastModificationTime;
	}

	/**
	 * Returns the creation time
	 * @return the creation time
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * Sets the the creation time
	 * @param creationTime the creation time
	 */
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * Returns the last access time
	 * @return the last access time
	 */
	public long getLastAccessTime() {
		return lastAccessTime;
	}

	/**
	 * Sets the last access time
	 * @param lastAccessTime the last access time
	 */
	public void setLastAccessTime(long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}
	
	@Override
	public String toString() {
		return getFullPath();
	}
}
