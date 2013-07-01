package jfilesyslib.utils;

/**
 * A small utility class which performs date conversions
 * @author Marc Miltenberger
 */
public final class DateUtils {
	private DateUtils() {
	}

	/**
	 * Returns a date object from a unix timestamp
	 * @param timeMs the unix timestamp
	 * @return the date object
	 */
	public static java.util.Date getDate(long timeMs) {
		return new java.util.Date((long)timeMs * 1000);
	}

	/**
	 * Returns a unix timestamp from a date object
	 * @param date the date object 
	 * @return the unix timestamp
	 */
	public static long getSeconds(java.util.Date date) {
		return date.getTime() / 1000L;
	}

	/**
	 * Returns the current unix timestamp
	 * @return the current unix timestamp
	 */
	public static long getNow() {
		return getSeconds(new java.util.Date());
	}
}
