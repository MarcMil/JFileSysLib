package jfilesyslib.exceptions;

/**
 * May be thrown if the given path is a file/symbolic link to a directory/symbolic link to a directory,
 * e.g. if openFile("/") is called
 * @author Marc Miltenberger
 */
public class NotAFileException extends Exception {
	private static final long serialVersionUID = 0;
	

}
