package jfilesyslib;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;


/**
 * Represents the current environment.
 * 
 * @author Marc Miltenberger
 */
public final class Environment {
	private static int userId = -1, groupId = -1;
	
	private Environment() {
	}
	
	private static int readId(String str)
	{
		try {
		    String userName = System.getProperty("user.name");
		    String command = "id " + str + " " + userName;
		    Process child = Runtime.getRuntime().exec(command);

		    // Get the input stream and read from it
		    InputStream in = child.getInputStream();
		    int c;
		    String s = "";
		    while ((c = in.read()) != -1) {
		        s += (char)c;
		    }
		    in.close();
		    return Integer.valueOf(s.replace("\n", "").trim());
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * Returns the unix user id of the current user.<br>
	 * If an error occurs, it returns the user id 0.
	 * @return the user id
	 */
	public static int getUserId()
	{
		if (userId == -1)
			userId = readId("-u");
		
		return userId;
	}
	
	/**
	 * Returns the unix group id of the current user.<br>
	 * If an error occurs, it returns the group id 0.
	 * @return the group id
	 */
	public static int getGroupId()
	{
		if (groupId == -1)
			groupId = readId("-g");
		
		return groupId;
	}
	
	/**
	 * Returns true if the JVM is running on a macintosh
	 * @return true if the JVM is running on a macintosh 
	 */
	public static boolean isMac(){
		java.lang.String os = System.getProperty("os.name").toLowerCase();
		//Mac
	    return (os.indexOf( "mac" ) >= 0); 
	}

	/**
	 * Returns true if the JVM is running on a unix like operating system
	 * @return true if the JVM is running on a unix like operating system
	 */
	public static boolean isUnix(){
		java.lang.String os = System.getProperty("os.name").toLowerCase();
		//linux or unix
	    return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);
	
	}
	
	/**
	 * Returns true if the JVM is running on Windows
	 * @return true if the JVM is running on Windows
	 */
	public static boolean isWindows(){
	
		java.lang.String os = System.getProperty("os.name").toLowerCase();
		//windows
	    return (os.indexOf( "win" ) >= 0); 
	
	}
	
	/**
	 * Returns true if the JVM is running on a 64 bit os
	 * @return true if the JVM is running on a 64 bit os
	 */
	public static boolean getIs64BitOperatingSystem() {
		java.lang.String s = System.getenv("PROCESSOR_ARCHITEW6432");
		if (s != null && s.contains("64"))
			return true;
		s = System.getenv("PROCESSOR_ARCHITECTURE");
		if (s != null)
			return s.contains("64");
		
		if (Environment.isUnix() || Environment.isMac())
		{
			try {
				Process p = Runtime.getRuntime().exec("uname -m");
				p.waitFor();
				byte[] b = new byte[p.getInputStream().available()];
				p.getInputStream().read(b);
				s = new java.lang.String(b, "UTF-8");
				if (s != null && s.length() > 2)
					return s.contains("64");
			} catch (java.lang.Exception e) {
			}
		}
		
		return getIs64BitJVM();
	}
	
	/**
	 * Returns true if the JVM is running on a 64 bit process
	 * @return true if the JVM is running on a 64 bit process
	 */
	public static boolean getIs64BitJVM() {
    	final java.lang.String keys [] = {
                "sun.arch.data.model",
                "com.ibm.vm.bitmode",
                "os.arch",
            };

        for (java.lang.String key : keys ) {
        	java.lang.String property = System.getProperty(key);
            if (property != null && !property.isEmpty()) {
                if (property.indexOf("64") >= 0)
                	return true;
                else
                	return false;
            }
        }
        return false;
	}
	
	/**
	 * Returns true if the JVM is running on an ARM processor
	 * @return true if the JVM is running on an ARM processor
	 */
	public static boolean getIsARM() {
		try {
			File file = new File("/proc/cpuinfo");
			FileReader stream = new FileReader(file);
			char[] buffer = new char[512];
			stream.read(buffer);
			stream.close();
			if (new String(buffer).contains("ARMv"))
				return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        return false;
	}
	
}