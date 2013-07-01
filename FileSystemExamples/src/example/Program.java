package example;

import java.io.File;
import java.io.IOException;

import jfilesyslib.FileSystem;
import jfilesyslib.Mounter;
import jfilesyslib.exceptions.NoDriveLetterLeftException;
import jfilesyslib.exceptions.TestFailedException;
import jfilesyslib.filesystems.ExtendedSupportFs;
import jfilesyslib.filesystems.MemoryFs;
import jfilesyslib.utils.TestFileSystem;

/**
 * @author Marc Miltenberger
 * Mounts the file system
 */
public class Program {

	/**
	 * The main methods
	 * @param args program arguments
	 */
	public static void main(String[] args) {
		File mountPath = null;
		FileSystem fsMount = new StaticFileSystem();
		
		
		
		//this line turns the static file system into a static file system with RAM-Disk :)
		fsMount = new jfilesyslib.filesystems.MergeDirectlyFs(new MemoryFs(), fsMount)
		{

			@Override
			public String getFileSystemName() {
				return "StaticFs";
			}

			@Override
			public String getVolumeName() {
				return "Static file system";
			}

		};

		
		//We want advanced features (symbolic links, extended attributes, file permissions, etc.)
		//w/o writing any extra code.
		fsMount = new ExtendedSupportFs(fsMount);
		
		
		try {
			TestFileSystem.testFileSystemUnmounted(fsMount, true);
		} catch (TestFailedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		//To enable the builtin logging feature, uncomment the following line
		//fsMount = new jfilesyslib.filesystems.LoggingFs(fsMount);
		
		if (args.length > 0)
			mountPath = new File(args[0]);
		else
		{
			try {
				mountPath = jfilesyslib.Mounter.chooseMountPath(fsMount);
			} catch (NoDriveLetterLeftException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		//We do not have a registration key...
		//jfilesyslib.JFileSysLibInfo.register("", "");
		
		jfilesyslib.Mounter.mount(fsMount, mountPath);

		Mounter.openExplorerWindow(mountPath);
		System.out.println("Press enter to unmount.");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Unmounting...");
		Mounter.unmount(fsMount);
		
		System.out.println("Unmounted");
	}

}
