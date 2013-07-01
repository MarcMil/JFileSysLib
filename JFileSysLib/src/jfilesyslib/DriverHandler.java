package jfilesyslib;

/**
 * Implement this interface if you want to react to driver problems.
 * 
 * @author Marc Miltenberger
 */
public interface DriverHandler {
	/**
	 * The Dokan Windows driver is not installed.
	 * 
	 * @param installer the path to the installer
	 */
	public void WindowsDriverNotFound(String installer);
	
	/**
	 * The unix FUSE connection was not found or could not be used .
	 */
	public void UnixFUSENotFound();

	/**
	 * The Mac OS X driver was not found.
	 * 
	 * @param installer the path to the installer
	 */
	public void MacOSXDriverNotFound(String installer);
}
