package jfilesyslib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JOptionPane;


/**
 * The default driver handler.<br>
 * Under Windows & Mac OS X: Installs the driver<br>
 * Under other Unix OSes: Displays an error message
 * @author Marc Miltenberger
 */
public class DefaultDriverHandler implements DriverHandler {
	/**
	 * Whether and how to ask before installing the driver
	 * @author Marc Miltenberger
	 */
	public enum Mode
	{
		/**
		 * GUI (Swing) 
		 */
		GUI,
		
		/**
		 * Displays a console message
		 */
		CONSOLE,
		
		/**
		 * Displays nothing
		 */
		SILENT
	};
	Mode usemode = Mode.GUI;
	
	/**
	 * Initializes a new default driver handler
	 * @param mode whether and how to ask before installing the driver
	 */
	public DefaultDriverHandler(Mode mode) {
		this.usemode = mode;
	}
	
	/**
	 * Initializes a new default driver handler
	 */
	public DefaultDriverHandler() {
		
	}

	/**
	 * Runs the Dokan installer
	 * @param installer the path to the installer
	 */
	public void runInstaller(String installer) {
		ProcessBuilder builder = new ProcessBuilder(installer, "/S");
		try {
			//for Windows XP:
			builder.start().waitFor();
		} catch (Exception e) {
			try {
				//for Windows 8:
				if (System.getProperty("os.version").equals("6.2"))
				{

					File file = new File(installer);
					File batch = new File(file.getParent(), "install.bat");
					FileWriter outFile = new FileWriter(batch);
					PrintWriter out = new PrintWriter(outFile);
					out.append("reg.exe Add \"HKCU\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AppCompatFlags\\Layers\" /v \"" + installer + "\" /d \"WIN7RTM ELEVATECREATEPROCESS RUNASADMIN DISABLENXSHOWUI\"\r\n");
					out.append(installer + " /S");
					out.close();
					outFile.close();
					ProcessBuilder builder2 = new ProcessBuilder(batch.getAbsolutePath());
					try {
						builder2.start().waitFor();
					} catch (Exception ex)
					{
						ex.printStackTrace();
					}
					//Elevator.executeAsAdministrator(batch.getAbsolutePath(), "");
				} else
				{
					//for Windows 7:
				//	Elevator.executeAsAdministrator(installer, "/S");
					//WIN7RTM 
					File file = new File(installer);
					File batch = new File(file.getParent(), "install.bat");
					FileWriter outFile = new FileWriter(batch);
					PrintWriter out = new PrintWriter(outFile);
					out.append("reg.exe Add \"HKCU\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AppCompatFlags\\Layers\" /v \"" + installer + "\" /d \"WIN7RTM ELEVATECREATEPROCESS RUNASADMIN DISABLENXSHOWUI\"\r\n");
					out.append(installer + " /S");
					out.close();
					outFile.close();
					ProcessBuilder builder2 = new ProcessBuilder(batch.getAbsolutePath());
					try {
						builder2.start().waitFor();
					} catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			} catch (Exception ex)
			{
				ex.printStackTrace();
				String text = "Please install Dokan from " + installer;
				switch (usemode)
				{
					case GUI:
						JOptionPane.showMessageDialog(null, text);
						break;
					case CONSOLE:
						System.out.println(text);
						break;
					default:
						break;
				}
			}
		}
	}
	
	@Override
	public void WindowsDriverNotFound(String installer) {
		String text = "Dokan is required in order to get the file system working.\r\n\r\nI'm launching the installer, which may require additional rights."; 
		switch (usemode)
		{
			case GUI:
				JOptionPane.showMessageDialog(null, text);
				break;
			case CONSOLE:
				System.out.println(text);
				break;
			default:
				break;
		}
		runInstaller(installer);
	}

	@Override
	public void UnixFUSENotFound() {
		String text = "Please compile Fuse4J libraries for your platform and architecture: https://github.com/dtrott/fuse4j\n";
		switch (usemode)
		{
			case GUI:
				JOptionPane.showMessageDialog(null, text);
				break;
			case CONSOLE:
				System.out.println(text);
				break;
			default:
				break;
		}
	}

	@Override
	public void MacOSXDriverNotFound(String installer) {
		String text = "fuse4x is required in order to get the file system working.\n\nI'm launching the installer, which requires additional rights."; 
		switch (usemode)
		{
			case GUI:
				JOptionPane.showMessageDialog(null, text);
				break;
			case CONSOLE:
				System.out.println(text);
				break;
			default:
				break;
		}
		String[] command = {
	    	        "osascript",
	    	        "-e",
	    	        "do shell script \"installer -verbose -pkg "  + installer + " -target /\" with administrator privileges" };
	    	Runtime runtime = Runtime.getRuntime();
	    	try {
	    	    Process process = runtime.exec(command);
//	    	    System.out.println(command[0] + " " + command[1] + " " + command[2]);
	    	    process.waitFor();
	    	} catch (IOException e) {
	    	    e.printStackTrace();
	    	} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

}
