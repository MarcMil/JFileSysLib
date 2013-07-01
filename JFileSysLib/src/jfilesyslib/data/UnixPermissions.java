package jfilesyslib.data;

import fuse.FuseStatConstants;

/**
 * Represents unix permissions
 * @author Marc Miltenberger
 */
public class UnixPermissions {
	private boolean ownerRead, ownerWrite, ownerExecute,
	 groupRead, groupWrite, groupExecute, othersRead,
	 othersWrite, othersExecute, suid, sgid,
	 sticky;
	
	private int uid, gid;
	
	/**
	 * The default directory permissions
	 */
	public final static UnixPermissions DefaultDirectoryPermissions = new UnixPermissions(true, true, true, true, true, true, true, true, true, false, false, false);
	
	/**
	 * The default file permissions
	 */
	public final static UnixPermissions DefaultFilePermissions = new UnixPermissions(true, true, false, true, true, false, true, true, false, false, false, false);
	

	/**
	 * Returns the owner user id
	 * @return the uid
	 */
	public int getUid() {
		return uid;
	}

	/**
	 * Sets the owner user id
	 * @param uid the uid to set
	 */
	public void setUid(int uid) {
		this.uid = uid;
	}

	/**
	 * Sets the owner group id
	 * @return the gid
	 */
	public int getGid() {
		return gid;
	}

	/**
	 * Returns the owner group id
	 * @param gid the gid to set
	 */
	public void setGid(int gid) {
		this.gid = gid;
	}


	/**
	 * Returns true if the owner may read
	 * @return true if the owner may read
	 */
	public boolean isOwnerRead() {
		return ownerRead;
	}

	/**
	 * Sets the owner read permission
	 * @param ownerRead whether the owner may read
	 */
	public void setOwnerRead(boolean ownerRead) {
		this.ownerRead = ownerRead;
	}

	/**
	 * Returns true if the owner may write
	 * @return true if the owner may write
	 */
	public boolean isOwnerWrite() {
		return ownerWrite;
	}

	/**
	 * Sets the owner write permission
	 * @param ownerWrite whether the owner may read
	 */
	public void setOwnerWrite(boolean ownerWrite) {
		this.ownerWrite = ownerWrite;
	}

	/**
	 * Returns true if the owner may execute
	 * @return true if the owner may execute
	 */
	public boolean isOwnerExecute() {
		return ownerExecute;
	}

	/**
	 * Sets the owner execute permission
	 * @param ownerExecute whether the owner may execute
	 */
	public void setOwnerExecute(boolean ownerExecute) {
		this.ownerExecute = ownerExecute;
	}
	

	/**
	 * Returns true if the group may read
	 * @return true if the group may read
	 */
	public boolean isGroupRead() {
		return groupRead;
	}

	/**
	 * Sets the group read permission
	 * @param groupRead whether the group may read
	 */
	public void setGroupRead(boolean groupRead) {
		this.groupRead = groupRead;
	}

	/**
	 * Returns true if the group may write
	 * @return true if the group may write
	 */
	public boolean isGroupWrite() {
		return groupWrite;
	}

	/**
	 * Sets the group write permission
	 * @param groupWrite whether the group may read
	 */
	public void setGroupWrite(boolean groupWrite) {
		this.groupWrite = groupWrite;
	}

	/**
	 * Returns true if the group may execute
	 * @return true if the group may execute
	 */
	public boolean isGroupExecute() {
		return groupExecute;
	}

	/**
	 * Sets the group execute permission
	 * @param groupExecute whether the group may execute
	 */
	public void setGroupExecute(boolean groupExecute) {
		this.groupExecute = groupExecute;
	}
	

	/**
	 * Returns true if the others may read
	 * @return true if the others may read
	 */
	public boolean isOthersRead() {
		return othersRead;
	}

	/**
	 * Sets the others read permission
	 * @param othersRead whether the others may read
	 */
	public void setOthersRead(boolean othersRead) {
		this.othersRead = othersRead;
	}

	/**
	 * Returns true if the others may write
	 * @return true if the others may write
	 */
	public boolean isOthersWrite() {
		return othersWrite;
	}

	/**
	 * Sets the others write permission
	 * @param othersWrite whether the others may read
	 */
	public void setOthersWrite(boolean othersWrite) {
		this.othersWrite = othersWrite;
	}

	/**
	 * Returns true if the others may execute
	 * @return true if the others may execute
	 */
	public boolean isOthersExecute() {
		return othersExecute;
	}

	/**
	 * Sets the others execute permission
	 * @param othersExecute whether the others may execute
	 */
	public void setOthersExecute(boolean othersExecute) {
		this.othersExecute = othersExecute;
	}

	/**
	 * Returns the suid bit.
	 * @see https://en.wikipedia.org/wiki/Setuid
	 * @return the suid
	 */
	public boolean isSuid() {
		return suid;
	}

	/**
	 * Sets the suid bit.
	 * @see https://en.wikipedia.org/wiki/Setuid
	 * @param suid the suid bit
	 */
	public void setSuid(boolean suid) {
		this.suid = suid;
	}

	/**
	 * Returns the sgid bit.
	 * @see https://en.wikipedia.org/wiki/Setuid
	 * @return the sgid
	 */
	public boolean isSgid() {
		return sgid;
	}

	/**
	 * Sets the sgid bit.
	 * @see https://en.wikipedia.org/wiki/Setuid
	 * @param suid the sgid bit
	 */
	public void setSgid(boolean sgid) {
		this.sgid = sgid;
	}

	/**
	 * Returns the sticky bit
	 * @see https://en.wikipedia.org/wiki/Sticky_bit
	 * @return the sticky
	 */
	public boolean isSticky() {
		return sticky;
	}

	/**
	 * Sets the sticky bit
	 * @see https://en.wikipedia.org/wiki/Sticky_bit
	 * @param sticky the sticky bit
	 */
	public void setSticky(boolean sticky) {
		this.sticky = sticky;
	}

	/**
	 * Creates a new UnixPermissions object
	 * @param permissions the permissions
	 */
	public UnixPermissions(int permissions)
	{
		this(permissions, 0, 0);
	}

	/**
	 * Creates a new UnixPermissions object
	 * @param permissions the permissions
	 * @param uid the owner user id
	 * @param gid the owner group id
	 */
	public UnixPermissions(int permissions, int uid, int gid) 
	{
		setPermissions(permissions);
		this.uid = uid;
		this.gid = gid;
		
	}

	/**
	 * Creates a new UnixPermissions object
	 * @param ownerR whether the owner may read
	 * @param ownerW whether the owner may write
	 * @param ownerX whether the owner may execute a file/read a directory
	 * @param groupR whether the group may read
	 * @param groupW whether the group may write
	 * @param groupX whether the group may execute a file/read a directory
	 * @param othersR whether others may read
	 * @param othersW whether others may write
	 * @param othersX whether others may execute a file/read a directory
	 * @param suid the SUID bit
	 * @param sgid the SGID bit
	 * @param sticky the sticky bit
	 */
	public UnixPermissions(boolean ownerR, boolean ownerW, boolean ownerX,
			boolean groupR, boolean groupW, boolean groupX, boolean othersR,
			boolean othersW, boolean othersX, boolean suid, boolean sgid,
			boolean sticky) {
		this(ownerR, ownerW, ownerX, groupR, groupW, groupX, othersR, othersW, othersX, suid, sgid, sticky, 0, 0);
	}
	
	
	/**
	 * Creates a new UnixPermissions object
	 * @param ownerR whether the owner may read
	 * @param ownerW whether the owner may write
	 * @param ownerX whether the owner may execute a file/read a directory
	 * @param groupR whether the group may read
	 * @param groupW whether the group may write
	 * @param groupX whether the group may execute a file/read a directory
	 * @param othersR whether others may read
	 * @param othersW whether others may write
	 * @param othersX whether others may execute a file/read a directory
	 * @param suid the SUID bit
	 * @param sgid the SGID bit
	 * @param sticky the sticky bit
	 * @param uid the owner user id
	 * @param gid the owner group id
	 */
	public UnixPermissions(boolean ownerR, boolean ownerW, boolean ownerX,
			boolean groupR, boolean groupW, boolean groupX, boolean othersR,
			boolean othersW, boolean othersX, boolean suid, boolean sgid,
			boolean sticky, int uid, int gid) {
		this.ownerRead = ownerR;
		this.ownerWrite = ownerW;
		this.ownerExecute = ownerX;

		this.groupRead = groupR;
		this.groupWrite = groupW;
		this.groupExecute = groupX;

		this.othersRead = othersR;
		this.othersWrite = othersW;
		this.othersExecute = othersX;
		
		this.suid = suid;
		this.sgid = sgid;
		this.sticky = sticky;
		
		this.uid = uid;
		this.gid = gid;
	}
	
	@Override
	public String toString()
	{
		String s = (ownerRead ? "r" : "-") + (ownerWrite ? "w" : "-") + (ownerExecute ? "x" : "-") + (groupRead ? "r" : "-") + (groupWrite ? "w" : "-") + (groupExecute ? "x" : "-") + (othersRead ? "r" : "-") + (othersWrite ? "w" : "-") + (othersExecute ? "x" : "-");
		s += " set UID: " + suid + ", set GID: " + sgid + ", sticky: " + sticky;
		return s;
	}
	
	/**
	 * Sets the permissions (but not the user id or group id).
	 * @param permissions the permissions in numeric notation
	 * @see https://en.wikipedia.org/wiki/File_system_permissions#Numeric_notation
	 */
	public void setPermissions(int permissions) {
		othersExecute = ((1 & permissions) == 1);
		groupExecute = ((8 & permissions) == 8);
		ownerExecute = ((64 & permissions) == 64);
		
		othersRead = ((4 & permissions) == 4);
		groupRead = ((32 & permissions) == 32);
		ownerRead = ((256 & permissions) == 256);
		
		othersWrite = ((2 & permissions) == 2);
		groupWrite = ((16 & permissions) == 16);
		ownerWrite = ((128 & permissions) == 128);
		
		suid = ((2048 & permissions) == 2048);
		sgid = ((1024 & permissions) == 1024);
		sticky = ((512 & permissions) == 512);
	}

	/**
	 * Returns the permissions as a number
	 * @return the permissions in numeric notation
	 * @see https://en.wikipedia.org/wiki/File_system_permissions#Numeric_notation
	 */
	public int getPermissions() {
		int perms = 0;
		if (ownerRead)
			perms |= FuseStatConstants.OWNER_READ;
		
		if (ownerWrite)
			perms |= FuseStatConstants.OWNER_WRITE;
		
		if (ownerExecute)
			perms |= FuseStatConstants.OWNER_EXECUTE;
		
		if (groupRead)
			perms |= FuseStatConstants.GROUP_READ;
		
		if (groupWrite)
			perms |= FuseStatConstants.GROUP_WRITE;
		
		if (groupExecute)
			perms |= FuseStatConstants.GROUP_EXECUTE;
		
		if (othersRead)
			perms |= FuseStatConstants.OTHER_READ;
		
		if (othersWrite)
			perms |= FuseStatConstants.OTHER_WRITE;
		
		if (othersExecute)
			perms |= FuseStatConstants.OTHER_EXECUTE;

		
		if (suid)
			perms |= FuseStatConstants.SUID_BIT;
		
		if (sgid)
			perms |= FuseStatConstants.SGID_BIT;
		
		if (sticky)
			perms |= FuseStatConstants.STICKY_BIT;
		

		return perms;
	}


	@Override
	public int hashCode() {
		return getPermissions();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnixPermissions other = (UnixPermissions) obj;
		if (gid != other.gid)
			return false;
		if (groupExecute != other.groupExecute)
			return false;
		if (groupRead != other.groupRead)
			return false;
		if (groupWrite != other.groupWrite)
			return false;
		if (othersExecute != other.othersExecute)
			return false;
		if (othersRead != other.othersRead)
			return false;
		if (othersWrite != other.othersWrite)
			return false;
		if (ownerExecute != other.ownerExecute)
			return false;
		if (ownerRead != other.ownerRead)
			return false;
		if (ownerWrite != other.ownerWrite)
			return false;
		if (sgid != other.sgid)
			return false;
		if (sticky != other.sticky)
			return false;
		if (suid != other.suid)
			return false;
		if (uid != other.uid)
			return false;
		return true;
	}

}
