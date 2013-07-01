package jfilesyslib;

/**
 * Contains some common mount options
 * @author Marc Miltenberger
 */
public class MountOptions {
	
	private boolean syncMounting = true;
	private DriveType drivetype = DriveType.HARD_DRIVE;
	private boolean UnixOwnerImpersonateNewFiles = true;
	private boolean UnixOwnerImpersonateAllFiles = false;
	private boolean MacOSXLocal = false;
	private int threadCount = 1;

	/**
	 * Returns the drive type (supported only on Windows)<p>
	 * The default value is <i>HARD_DRIVE</i>.
	 * @return the drive type (supported only on Windows)<p>
	 */
	public DriveType getDrivetype() {
		return drivetype;
	}

	/**
	 * Sets the drive type (supported only on Windows)<p>
	 * The default value is <i>HARD_DRIVE</i>.
	 * @param drivetype the drive type
	 */
	public void setDrivetype(DriveType drivetype) {
		this.drivetype = drivetype;
	}

	/**
	 * Returns true if the mounting process should be synchronous.<br>
	 * Then the mount method does only return true if the mounting process has been completed successfully<br>
	 * <b>and</b> the file system is usable<p>
	 * This feature is turned <i>on</i> by default.
	 * @return true iff synchronous mounting is enabled
	 */
	public boolean isSyncMounting() {
		return syncMounting;
	}

	/**
	 * If the actual parameter is true, the mount method acts synchronous.<br>
	 * It does only return true if the mounting process has been completed successfully<p>
	 * This feature is turned <i>on</i> by default.
	 *
	 * @param syncMounting the new value
	 */
	public void setSyncMounting(boolean syncMounting) {
		this.syncMounting = syncMounting;
	}

	/**
	 * If this feature is turned on, new files and directories will have the user, who has mounted the file system as their owner.<p>
	 * This feature is turned <i>on</i> by default.
	 * @return the unixOwnerImpersonate value
	 */
	public boolean isUnixOwnerImpersonateNewFiles() {
		return UnixOwnerImpersonateNewFiles;
	}

	/**
	 * If this feature is turned on, new files and directories will have the user, who has mounted the file system as their owner.<p>
	 * This feature is turned <i>on</i> by default.
	 * @param unixOwnerImpersonate the new value
	 */
	public void setUnixOwnerImpersonateNewFiles(boolean unixOwnerImpersonate) {
		UnixOwnerImpersonateNewFiles = unixOwnerImpersonate;
	}

	/**
	 * If this feature is turned on, all files and directories will have the user, who has mounted the file system as their owner.<p>
	 * This feature is turned <i>off</i> by default.
	 * @return the unixOwnerImpersonate value
	 */
	public boolean isUnixOwnerImpersonateAllFiles() {
		return UnixOwnerImpersonateAllFiles;
	}

	/**
	 * If this feature is turned on, all files and directories will have the user, who has mounted the file system as their owner.<p>
	 * This feature is turned <i>off</i> by default.
	 * @param unixOwnerImpersonate the new value
	 */
	public void setUnixOwnerImpersonateAllFiles(boolean unixOwnerImpersonate) {
		UnixOwnerImpersonateAllFiles = unixOwnerImpersonate;
	}

	/**
	 * Returns the number of threads used for the file system
	 * @return the thread count
	 */
	public int getThreadCount() {
		return threadCount;
	}

	/**
	 * Sets the number of threads to use 
	 * @param threadCount the thread count
	 */
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	/**
	 * 
	 * Returns whether the file system should be mounted with the local flag on Mac OS X<br>
	 * This feature is turned <i>off</i> by default, because it may cause various problems.
	 * @return whether the file system should be mounted with the local flag
	 * @see http://fuse4x.github.com/options.html#local
	 */
	public boolean isMacOSXLocal() {
		return MacOSXLocal;
	}

	/**
	 * 
	 * Sets whether the file system should be mounted with the local flag on Mac OS X<br>
	 * This feature is turned <i>off</i> by default, because it may cause various problems.
	 * @param macOSXLocal whether the local flag should be set
	 * @see http://fuse4x.github.com/options.html#local
	 */
	public void setMacOSXLocal(boolean macOSXLocal) {
		MacOSXLocal = macOSXLocal;
	}
}
