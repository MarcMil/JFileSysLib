package jfilesyslib.data;

import jfilesyslib.utils.FileSystemUtils;

/**
 * Represents an extended attribute.<br>
 * Extended attributes may be used by the operating system to save additional meta data about the files.
 * @author Marc Miltenberger
 */
public class ExtendedAttribute {
	private String name;
	private byte[] content = new byte[0];
	
	/**
	 * The default constructor
	 */
	public ExtendedAttribute()
	{		
	}
	
	/**
	 * Initializes a new instance of ExtendedAttribute
	 * @param name the name of the extended attribute
	 * @param content the content of the extended attribute
	 */
	public ExtendedAttribute(String name, byte[] content) {
		this.name = name;
		this.content = content;
	}
	
	/**
	 * Returns the name of the extended attribute
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the extended attribute
	 * @param name the name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the content
	 * @return the content
	 */
	public byte[] getContent() {
		return content;
	}
	
	/**
	 * Sets the content
	 * @param content the content
	 */
	public void setContent(byte[] content) {
		this.content = content;
	}
	

	@Override
    public int hashCode() {
        int code = name.hashCode();
        for (byte b : content)
        	code ^= b;
        return code;
    }

	@Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;
        
        ExtendedAttribute attr = (ExtendedAttribute)obj;
        return attr.getName().equals(name) && FileSystemUtils.simpleByteArrayCompare(attr.content, content);
    }
}
