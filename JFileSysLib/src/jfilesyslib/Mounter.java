package jfilesyslib;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jfilesyslib.exceptions.NoDriveLetterLeftException;
import net.decasdev.dokan.DokanOptions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fuse.FuseMount;

/**
 * Use this class to mount your virtual file system.
 * @author Marc Miltenberger
 */
public final class Mounter {
	private static final int DOKAN_OPTION_KEEP_ALIVE = 8;
	private static final int DOKAN_OPTION_NETWORK = 16;
	private static final int DOKAN_OPTION_REMOVABLE = 32;
	
	private Mounter() {
	}
	
	/**
	 * Loads the native files for windows
	 */
	private static void loadNativeWindows()
	{
		String path = "/native/windows";
		String libPath;
		if (Environment.getIs64BitJVM())
			path += "/JDokan-x64.dll";
		else
			path += "/JDokan-x86.dll";
		
		try {
			libPath = writeToTempPath(path, "JDokan.dll").getAbsolutePath(); //.getParentFile().getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.load(libPath);

	}
	
	/**
	 * Automatically chooses an available mount path
	 * @param system the file system
	 * @return the mount path
	 */
	public static File chooseMountPath(FileSystem system) throws NoDriveLetterLeftException
	{
		if (Environment.isWindows())
		{
			String letters = "CDEFGHIJKLMNOPQRSTUVWXYZ";
			File[] roots = File.listRoots();
			for (int i = 0; i < letters.length(); i++)
			{
				File drive = new File(letters.substring(i, i + 1) + ":");
				boolean continues = true;
				for (File root : roots)
				{
					if (root.getAbsolutePath().charAt(0) == drive.getAbsolutePath().charAt(0))
					{
						continues = false;
						break;
					}
				}
				
				if (!continues)
					continue;
				
				if (!isMounted(drive))
					return drive;
			}
			throw new NoDriveLetterLeftException();
		} else {
			String[] pathes = { "/media/", "/mnt/", System.getProperty("user.home"), "/tmp/", System.getProperty("user.dir") };
			for (String path : pathes)
			{
				File root = new File(path);
				if (!root.canWrite() || !root.isDirectory())
					continue;
				
				String volumeName = system.getVolumeName();
				volumeName = volumeName.replace("/", "-");
				File subdir = new File(root, volumeName);
				File tempFile = new File(subdir, ".tmpDelete");
				if (subdir.exists())
				{
					if (!subdir.isDirectory())
						continue;
					
					if (isMounted(subdir))
						continue;
					
					if (!tempFile.exists())
					{
						if (subdir.listFiles().length > 0)
							continue;
					}
				} 
				else
				{
					if (!subdir.mkdir())
						continue;
					subdir.deleteOnExit();
				}
				if (tempFile.exists())
				{
					tempFile.deleteOnExit();
					subdir.deleteOnExit();
					tempFile.deleteOnExit();
				}
				else
				{
					try {
						new FileOutputStream(tempFile).close();
					} catch (IOException e) {
					}
				}
				return subdir;
			}
			throw new NoDriveLetterLeftException();
		}
	}
	
	/**
	 * Mounts the specified file system on the mount path
	 * @param fileSystem the file system
	 * @param mountPath the mount path
	 * @return true if the operation was successful
	 */
	public static boolean mount(FileSystem fileSystem, final File mountPath)
	{
		return mount(fileSystem, mountPath, new MountOptions(), new DefaultDriverHandler());
	}
	
	/**
	 * Mounts the specified file system on the mount path
	 * @param fileSystem the file system
	 * @param mountPath the mount path
	 * @param options mount options
	 * @return true if the operation was successful
	 */
	public static boolean mount(FileSystem fileSystem, final File mountPath, final MountOptions options)
	{
		return mount(fileSystem, mountPath, options, new DefaultDriverHandler());
	}
	
	/**
	 * Mounts the specified file system on the mount path
	 * @param fileSystem the file system
	 * @param mountPath the mount path
	 * @param options mount options
	 * @param driverHandler handles the situation in case the driver loading process fails
	 * @return true if the operation was successful
	 */
	public static boolean mount(FileSystem fileSystem, final File mountPath, final MountOptions options, final DriverHandler driverHandler)
	{
		final FileSystem cfileSystem = fileSystem;
		if (Environment.isWindows())
		{
			if (mountPath.getAbsolutePath().length() > 3 && !mountPath.exists())
				throw new IllegalArgumentException("The mountPath (" + mountPath + ") should be a drive");
			
			if (mountPath.isFile())
				throw new IllegalArgumentException("The mountPath (" + mountPath + ") should be a drive");
			loadNativeWindows();
			cfileSystem.mountPath = mountPath.getAbsolutePath();
			final DokanOptions doptions = new DokanOptions();
			doptions.mountPoint = mountPath.getAbsolutePath();
			doptions.optionsMode = DOKAN_OPTION_KEEP_ALIVE;
			doptions.threadCount = options.getThreadCount();
			switch (options.getDrivetype())
			{
				case NETWORK_SHARE:
					doptions.optionsMode |= DOKAN_OPTION_NETWORK;
					break;
				case REMOVABLE_DRIVE:
					doptions.optionsMode |= DOKAN_OPTION_REMOVABLE;
					break;
				default:
					break;
			}
			final DokanWrapper operations = new DokanWrapper(cfileSystem);

			Thread thrMount = new Thread(new Runnable() {

				@Override
				public void run() {
					cfileSystem.beforeMounting(mountPath.getAbsolutePath());
					int res = net.decasdev.dokan.Dokan.mount(doptions, operations);
					//net.decasdev.dokan.Dokan.removeMountPoint(mountPath.getAbsolutePath());
					if (res == net.decasdev.dokan.Dokan.DOKAN_DRIVER_INSTALL_ERROR)
					{
						try {
							String dokanPath = writeToTempPath("/native/windows/DokanInstall_0.6.0.exe", "DokanInstall_0.6.0.exe").getAbsolutePath();
							driverHandler.WindowsDriverNotFound(dokanPath);
							for (int i = 0; i < 20; i++)
							{
								Thread.sleep(1000);
								res = net.decasdev.dokan.Dokan.mount(doptions, operations);
								if (res == 0)
									break;
							}
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} 
					}
				}
			});
			thrMount.setDaemon(true);
			cfileSystem.thrMounted = thrMount;
			thrMount.start();
			
			if (options.isSyncMounting())
			{
				File file = new File(mountPath, DokanWrapper.INITIALFILENAME);
				while (!file.exists())
				{
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				//Windows needs some more time...
				//Slow windows :)
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			operations.setInitial(false);
		} else {
			if (!mountPath.isDirectory())
				throw new IllegalArgumentException("The mountPath (" + mountPath + ") should be a directory");
			cfileSystem.mountPath = mountPath.getAbsolutePath();
			if (!mountPath.canWrite())
				return false;
			final FuseWrapper wrapper = new FuseWrapper(cfileSystem, options);
			String path, filename;
			if (Environment.isMac())
			{
				path = "/native/mac/";
				filename = "libjavafs.jnilib";
				path += filename;
			}
			else
			{
				path = "/native/linux-";
			
				if (Environment.getIs64BitJVM())
				{
					path += "amd64";
				}
				else
				{
					if (Environment.getIsARM())
						path += "armhf";
					else
						path += "i386";
				}
				filename = "libjavafs.so";
				path += "/" + filename;
			}
			
			String libPath;
			try {
				libPath = writeToTempPath(path, filename).getAbsolutePath(); //.getParentFile().getAbsolutePath();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			try {
				System.load(libPath);
			} catch (UnsatisfiedLinkError err)
			{
				if (Environment.isMac())
				{
					try {
						installMacDriver(driverHandler);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					try {
						System.load(libPath);
					} catch (Exception exc)
					{
						exc.printStackTrace();
					}
				} else {
					err.printStackTrace();
					driverHandler.UnixFUSENotFound();
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				driverHandler.UnixFUSENotFound();
			} 
			
			Thread thrMount = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						cfileSystem.beforeMounting(mountPath.getAbsolutePath());
						Log l = LogFactory.getLog("Mounter");
						if (Environment.isUnix())
							FuseMount.mount(new String[] { cfileSystem.getFileSystemName(), mountPath.getAbsolutePath(), "-f", "-o", "nonempty" }, wrapper, l);
						else
						{
							String[] mountOptions;
							if (options.isMacOSXLocal())
								mountOptions = new String[] { cfileSystem.getFileSystemName(), mountPath.getAbsolutePath(), "-f", "-o", "volname=" + cfileSystem.getVolumeName(), "-o", "fsname=" + cfileSystem.getFileSystemName(), "-o", "local" };
							else
								mountOptions = new String[] { cfileSystem.getFileSystemName(), mountPath.getAbsolutePath(), "-f", "-o", "volname=" + cfileSystem.getVolumeName(), "-o", "fsname=" + cfileSystem.getFileSystemName() };
							FuseMount.mount(mountOptions, wrapper, l);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			});
			cfileSystem.thrMounted = thrMount;
			thrMount.setDaemon(true);
			thrMount.start();

			if (options.isSyncMounting())
			{
				File file = new File(mountPath, DokanWrapper.INITIALFILENAME);
				while (!file.exists())
				{
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			wrapper.setInitial(false);
		}
		return true;
	}
	
	private static void installMacDriver(DriverHandler driverHandler) throws IOException {
		String path = writeToTempPath("/native/mac/Fuse4X.pkg.zip", "Fuse4X.pkg.zip").getAbsolutePath();	

		File tempFile = File.createTempFile("native", "f");
		tempFile.delete();
		tempFile.mkdirs();
		tempFile.deleteOnExit();
		tempFile = new File(tempFile, "Fuse4X.pkg");
		//System.out.println(tempFile.getAbsolutePath());
		ZipFile zipFile = new ZipFile(path);

	      Enumeration<? extends ZipEntry> entries = zipFile.entries();

	      while (entries.hasMoreElements()) {
	        ZipEntry entry = (ZipEntry) entries.nextElement();

	        if(entry.isDirectory()) {
	          (new File(tempFile, entry.getName())).mkdirs();
	          continue;
	        }

	        copyStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(new File(tempFile, entry.getName()))));
	      }

	      zipFile.close();
	      
	      driverHandler.MacOSXDriverNotFound(new File(tempFile.getAbsolutePath(), "Fuse4X.pkg").getAbsolutePath());
	}

	private static final void copyStream(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[1024];
	    int len;
	
	    while ((len = in.read(buffer)) >= 0)
	    	out.write(buffer, 0, len);
	
	    in.close();
	    out.close();
	}
	  
	  
	/**
	 * Tries to check whether the mount path is already in use.
	 * @param mountPath the mount path
	 * @return true if the mount path is in use
	 */
	public static boolean isMounted(File mountPath)
	{
		if (Environment.isWindows())
		{
			File[] roots = File.listRoots();
			for (File f : roots)
				if (f.getAbsolutePath().charAt(0) == mountPath.getAbsolutePath().charAt(0))
					return true;
			return false; // mountPath.exists();
		}
		try {
		    Process child = Runtime.getRuntime().exec("mount");

		    InputStream in = child.getInputStream();
		    int c;
		    String s = "";
		    while ((c = in.read()) != -1) {
		        s += (char)c;
		    }
		    in.close();
		    return s.contains(mountPath.getAbsolutePath());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Unmounts the specified mount path.<br>
	 * You should use the filesystem overloading since beforeUnmounting() and afterUnmounting() are not being called.
	 * @param mountpath the mount path
	 * @return true if the unmounting process has completed successfully
	 */
	public static boolean unmount(File mountPath)
	{
		boolean res;
		if (Environment.isWindows())
		{
			res = net.decasdev.dokan.Dokan.removeMountPoint(mountPath.getAbsolutePath().substring(0, 2));
			if (res)
			{
				int max = 0;
				while (isMounted(mountPath))
				{
					try {
						Thread.sleep(10);
						max++;
						if (max == 200)
							break;
					} catch (InterruptedException e) {
					}
				}
			}
		}
		else {
			if (Environment.isMac())
			{
				int result = -1;
				ProcessBuilder builder2 = new ProcessBuilder("diskutil", "unmount", "force", mountPath.getAbsolutePath());
				try {
					result = builder2.start().waitFor();
				} catch (Exception e) {
				}
				res = (result == 0);
			} else {
				ProcessBuilder builder = new ProcessBuilder("fusermount", "-zu", mountPath.getAbsolutePath());
				int result = -1;
				try {
					result = builder.start().waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				res = (result == 0);
			}
		}
		if (res)
		{
			File tempFile = new File(mountPath, ".tmpDelete");
			if (tempFile.exists())
				tempFile.delete();
		}
		return res;
	}
	
	/**
	 * Unmounts the specified file system
	 * @param fileSystem the file system
	 * @return true if the unmounting process has completed successfully
	 */
	public static boolean unmount(FileSystem fileSystem)
	{
		String mountPath = fileSystem.getMountPath();
		if (mountPath == null)
			return false;
		fileSystem.beforeUnmounting();
		
		
		unmount(new File(mountPath));

		fileSystem.mountPath = null;
		if (!isMounted(new File(mountPath)))
		{
			fileSystem.afterUnmounting();
			return true;
		}
		else
		{
			if (Environment.isWindows())
			{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				if (!isMounted(new File(mountPath)))
					return true;
			}
			return false;
		}

	}
	
	private static File writeToTempPath(String resourceName, String path) throws IOException
	{
		String suffix = resourceName;
		if (suffix.contains("."))
			suffix = suffix.substring(suffix.lastIndexOf('.'));
		File tempFile = File.createTempFile("native", "no");
		tempFile.delete();
		tempFile.mkdirs();
		tempFile.deleteOnExit();
		tempFile = new File(tempFile, path);

		InputStream streamIn = Mounter.class.getResourceAsStream(resourceName);
		FileOutputStream streamOut = new FileOutputStream(tempFile);
		byte[] buffer = new byte[1024 * 1024];
		while (true)
		{
			int read = streamIn.read(buffer);
			if (read <= 0)
				break;
			streamOut.write(buffer, 0, read);
		}
		streamOut.close();
		streamIn.close();
		return tempFile;
	}

	/**
	 * A small utility function which opens the file explorer
	 * @param mountPath the path to open
	 */
	public static boolean openExplorerWindow(final File mountPath) {
		try {
			Desktop.getDesktop().open(mountPath);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
