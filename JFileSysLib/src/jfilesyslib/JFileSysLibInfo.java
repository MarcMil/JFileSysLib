package jfilesyslib;


/**
 * Contains library related information and a simple dummy main method. 
 * @author Marc Miltenberger
 */
public class JFileSysLibInfo {
	/**
	 * The version of the userspace file system library
	 */
	public static final String VERSION = Messages.getString("JFileSysLibInfo.VersionString"); //$NON-NLS-1$

	/**
	 * The author
	 */
	public static final String AUTHOR = Messages.getString("JFileSysLibInfo.AuthorString"); //$NON-NLS-1$
	

	/**
	 * The URL
	 */
	public static final String URL = Messages.getString("JFileSysLibInfo.URL"); //$NON-NLS-1$
	
	
	/**
	 * A dummy main method which only writes the version to stdout.
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		System.out.println(Messages.getString("JFileSysLibInfo.LongVersionString").replace("$VERSION$", VERSION)); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(AUTHOR);
		System.out.println(URL);
	}
	
	

}
