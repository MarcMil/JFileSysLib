package jfilesyslib.data;

/**
 * This class should be used to model symbolic links
 * @author Marc Miltenberger
 */
public class SymbolicLinkInfo extends EntityInfo {
	public String destination;
	
	/**
	 * Creates a new SymbolicLinkInfo object.
	 * @param source the source of the symbolic link
	 * @param destination the destination the symbolic link points to
	 */
	public SymbolicLinkInfo(String source, String destination)
	{
		this.destination = destination;
		this.setFullPath(source);
	}
}
