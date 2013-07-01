package jfilesyslib.exceptions;

/**
 * May be thrown if the given path is a file/symbolic link to a file,
 * e.g. if readDirectory("/a file") is called
 * @author Marc Miltenberger
 */
public class NotADirectoryException extends Exception {
	private static final long serialVersionUID = 0;


}
