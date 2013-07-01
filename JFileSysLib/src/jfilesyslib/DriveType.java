package jfilesyslib;

/**
 * The drive type
 */
public enum DriveType {
	/**
	 * Mounts the file system as a network share
	 */
	NETWORK_SHARE,
	
	/**
	 * Mounts the file system as a removable drive
	 */
	REMOVABLE_DRIVE,
	
	/**
	 * Mounts the file system as a hard drive
	 */
	HARD_DRIVE
}