package jfilesyslib.exceptions;

/**
 * May be thrown if the given path was not found
 * e.g. if readDirectory("/does not exist") is called
 * @author Marc Miltenberger
 */
public class DriveFullException extends Exception {
	private static final long serialVersionUID = 0;


}
