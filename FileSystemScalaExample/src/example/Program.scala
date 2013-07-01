package example
import jfilesyslib.FileSystem
import jfilesyslib.filesystems._
import jfilesyslib.exceptions._
import jfilesyslib.utils._
import jfilesyslib._

object Program {
  def main(args : Array[String]) {
    var mountPath : java.io.File = null
    
    var fsMount : FileSystem = new StaticFileSystem()
    
	//this line turns the static file system into a static file system with RAM-Disk :)
	fsMount = new jfilesyslib.filesystems.MergeDirectlyFs(new MemoryFs(), fsMount)
	{
      override def getFileSystemName() : String = {
        "StaticFs";
      }
	
      override def getVolumeName() : String = {
	    "Static file system";
	  }
	
	};
	

	//We want advanced features (symbolic links, extended attributes, file permissions, etc.)
	//w/o writing any extra code.
	fsMount = new ExtendedSupportFs(fsMount)
	
	try {
		TestFileSystem.testFileSystemUnmounted(fsMount, true)
	} catch {
	  case e : TestFailedException  =>
	    {
		e.printStackTrace();
		System.exit(1);
	    }
	}
	
	//To enable the builtin logging feature, uncomment the following line
	//fsMount = new jfilesyslib.filesystems.LoggingFs(fsMount);
	
	
	if (args.length > 0)
	  mountPath = new java.io.File(args(0))
	else
	  mountPath = Mounter.chooseMountPath(fsMount)
	  
	//We do not have a registration key...
	//jfilesyslib.JFileSysLibInfo.register("", "");
	
	Mounter.mount(fsMount, mountPath)
	Mounter.openExplorerWindow(mountPath)
	Console.println("Press enter to unmount.")
	
	Console.readLine
	
	Console.println("Unmounting...");
	Mounter.unmount(fsMount)
	Console.println("Unmounted");
  }
}