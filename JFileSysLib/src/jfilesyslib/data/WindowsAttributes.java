package jfilesyslib.data;


/**
 * Represents Windows attributes
 * @author Marc Miltenberger
 */
public class WindowsAttributes {
	private boolean archive, compressed, encrypted, hidden, notContentIndexed, offline, readonly, temporary;

	/**
	 * The default windows attributes
	 */
	public final static WindowsAttributes DefaultWindowsAttributes = new WindowsAttributes(false, false, false, false, false, false, false, false);

	/**
	 * The default windows attributes with the read only flag set
	 */
	public final static WindowsAttributes ReadOnlyWindowsAttributes = new WindowsAttributes(false, false, false, false, false, false, true, false);

	
	/**
	 * The default constructor
	 */
	public WindowsAttributes()
	{
	}
	
	/**
	 * Creates a new WindowsAttributes instance
	 * @param archive the archive flag
	 * @param compressed the compressed flag
	 * @param encrypted the encrypted flag
	 * @param hidden the hidden flag
	 * @param notContentIndexed the notContentIndexed flag 
	 * @param offline the offline flag
	 * @param readonly the readonly flag
	 * @param temporary the temporary flag
	 */
	public WindowsAttributes(boolean archive, boolean compressed, boolean encrypted, boolean hidden, boolean notContentIndexed, boolean offline, boolean readonly, boolean temporary)
	{
		this.archive = archive;
		this.compressed = compressed;
		this.encrypted = encrypted;
		this.hidden = hidden;
		this.notContentIndexed = notContentIndexed;
		this.offline = offline;
		this.readonly = readonly;
		this.temporary = temporary;
	}
	
	/**
	 * Creates a new WindowsAttributes instance
	 * @param fileAttributes the file attributes
	 */
	public WindowsAttributes(int fileAttributes)
	{
		archive = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_ARCHIVE) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_ARCHIVE);
		compressed = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_COMPRESSED) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_COMPRESSED);
		encrypted = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_ENCRYPTED) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_ENCRYPTED);
		hidden = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_HIDDEN) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_HIDDEN);
		notContentIndexed = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED);
		offline = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_OFFLINE) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_OFFLINE);
		readonly = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_READONLY) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_READONLY);
		temporary = ((fileAttributes & net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_TEMPORARY) == net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_TEMPORARY);
	}


	/**
	 * Returns the attributes as a number
	 * @return the attributes
	 */
	public int getAttributes() {
		int perms = 0;
		
		if (archive)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_ARCHIVE;
		
		if (compressed)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_COMPRESSED;
		
		if (encrypted)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_ENCRYPTED;
		
		if (hidden)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_HIDDEN;
		
		if (notContentIndexed)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NOT_CONTENT_INDEXED;
		
		if (offline)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_OFFLINE;
		
		if (readonly)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_READONLY;
		
		if (temporary)
			perms |= net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_TEMPORARY;

		return perms;
	}

	/**
	 * Returns the archive flag
	 * @return the archive flag
	 */
	public boolean isArchive() {
		return archive;
	}

	/**
	 * Sets the archive flag
	 * @param archive the archive flag
	 */
	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	/**
	 * Returns the compressed flag
	 * @return the compressed flag
	 */
	public boolean isCompressed() {
		return compressed;
	}

	/**
	 * Sets the compressed flag
	 * @param compressed the compressed to set
	 */
	public void setCompressed(boolean compressed) {
		this.compressed = compressed;
	}

	/**
	 * Returns the encrypted flag
	 * @return the encrypted
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * Sets the encrypted flag
	 * @param encrypted the encrypted to set
	 */
	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	/**
	 * Returns the hidden flag
	 * @return the hidden flag
	 */
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * Sets the hidden flag
	 * @param hidden the hidden to set
	 */
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * Returns the notContentIndexed flag
	 * @return the notContentIndexed flag
	 */
	public boolean isNotContentIndexed() {
		return notContentIndexed;
	}

	/**
	 * Sets the ContentIndexed flag
	 * @param notContentIndexed the notContentIndexed to set
	 */
	public void setNotContentIndexed(boolean notContentIndexed) {
		this.notContentIndexed = notContentIndexed;
	}

	/**
	 * Returns the offline flag
	 * @return the offline flag
	 */
	public boolean isOffline() {
		return offline;
	}

	/**
	 * Sets the offline flag
	 * @param offline the offline to set
	 */
	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	/**
	 * Returns the readonly flag
	 * @return the readonly
	 */
	public boolean isReadonly() {
		return readonly;
	}

	/**
	 * Sets the readonly flag
	 * @param readonly the readonly to set
	 */
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	/**
	 * @return the temporary
	 */
	public boolean isTemporary() {
		return temporary;
	}

	/**
	 * @param temporary the temporary to set
	 */
	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}
	


	@Override
    public int hashCode() {
        return getAttributes();
    }

	@Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;
        
        WindowsAttributes attr = (WindowsAttributes)obj;
        return attr.archive == archive && attr.compressed == compressed && attr.encrypted == encrypted && attr.hidden == hidden && attr.notContentIndexed == notContentIndexed && attr.offline == offline && attr.readonly == readonly && attr.temporary == temporary;
    }
	
	@Override
	public String toString()
	{
		return "Archive: " + archive + ", Compressed: " + compressed + ", Encrypted: " + encrypted + ", Hidden: " + hidden + ", NotContentIndexed: " + notContentIndexed + ", Offline: " + offline + ", ReadOnly: " + readonly + ", Temporary: " + temporary;
	}
}
