package jfilesyslib.data;

/**
 * This class should be used to model directories
 * @author Marc Miltenberger
 */
public class DirectoryInfo extends EntityInfo {
	/**
	 * Creates a new DirectoryInfo object.
	 * @param fullPath the full path to the directory. e.g.: "/New Directory"
	 */
	public DirectoryInfo(String fullPath)
	{
		if (fullPath.endsWith("/"))
			fullPath = fullPath.substring(0, fullPath.length() - 1);
		this.setFullPath(fullPath);
	}
}
