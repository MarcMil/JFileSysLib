package jfilesyslib.exceptions;

/**
 * May be thrown if the given path was not found
 * e.g. if readDirectory("/does not exist") is called
 * @author Marc Miltenberger
 */
public class PathNotFoundException extends Exception {
	private static final long serialVersionUID = 0;
	private String path;
	
	/**
	 * Creates a new path not found exception
	 * @param path the path
	 */
	public PathNotFoundException(String path)
	{
		this.path = path;
	}

	/**
	 * Returns the missing path
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
}
