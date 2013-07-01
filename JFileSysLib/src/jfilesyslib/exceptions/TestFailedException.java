package jfilesyslib.exceptions;

/**
 * May be thrown if the file system test failed
 * @author Marc Miltenberger
 */
public class TestFailedException extends Exception {
	/**
	 * Creates a new instance of TestFailedException
	 * @param reason the reason
	 */
	public TestFailedException(String reason) {
		super(reason);
	}

	/**
	 * Creates a new instance of TestFailedException
	 * @param exception the inner exception
	 */
	public TestFailedException(Exception exception) {
		super(exception);
	}

	/**
	 * Creates a new instance of TestFailedException
	 * @param reason the reason
	 * @param exception the inner exception
	 */
	public TestFailedException(String reason, Exception exception) {
		super(reason, exception);
	}

	private static final long serialVersionUID = 0;


}
