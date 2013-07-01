package example

import jfilesyslib.FileSystem
import jfilesyslib.exceptions._
import jfilesyslib.data._
import java.nio.ByteBuffer
import java.util.LinkedList
import java.io.InputStream

class File(var name : String, var content : Array[Byte]) {
  def generateFileInfo() : FileInfo = {
    new FileInfo(name, content.length)
  } 
}

object utils {
  def readByteArray(inputstream : InputStream) = {
    Stream.continually(inputstream.read).takeWhile(-1 !=).map(_.toByte).toArray
  }
}

class StaticFileSystem extends FileSystem {
  private var staticFiles = Array(new File("/Hello World.txt", "This is just a simple test file system.".getBytes()), new File("/Music Box.ogg", utils.readByteArray(getClass.getResourceAsStream("MusicBox.ogg"))))
  
  
  
  override def getBlockSize() : Int = {
    1024 * 4
  }

  override def getFreeBlockAvailableCount() : Long = {
    0
  }

  override def getFreeBlockCount() : Long = {
    0
  }

  override def getTotalBlockCount() : Long = {
    0
  }

  override def getVolumeName() : String = {
    "Static file system"
  }

  override def isCaseSensitive() : Boolean = {
    true
  }

  override def listDirectory(path : String) : java.lang.Iterable[EntityInfo] = {
    val files = new LinkedList[EntityInfo]();
    staticFiles.foreach(file => {
      files.add(file.generateFileInfo)
    })
    files.asInstanceOf[java.lang.Iterable[EntityInfo]]
  }
  
  override def getFileMetaData(path : String) : EntityInfo = {
    if (path == "/")
      new DirectoryInfo("/")
    else
      getFile(path).generateFileInfo
  }
  
  override def rename(source : String, destination : String) = {
    throw new AccessDeniedException();
  }

  override def setCreationTime(path : String, ctime : Long) = {
    throw new AccessDeniedException();
  }

  override def setLastModificationTime(path : String, ctime : Long) = {
    throw new AccessDeniedException();
  }

  override def setLastAccessTime(path : String, ctime : Long) = {
    throw new AccessDeniedException();
  }

  override def getFileSystemName() : String = {
    "StaticFs"
  }

  override def deleteDirectoryRecursively(path : String) = {
    throw new AccessDeniedException();
  }

  override def deleteFile(path : String) = {
    throw new AccessDeniedException();
  }

  override def openFile(file : String, read : Boolean, write : Boolean) : FileHandle = {
    if (file == "/")
      throw new NotAFileException
      
    val f = getFile(file)
    
    if (write)
      throw new AccessDeniedException()
    
    val handle = new FileHandle(file)
    handle.setObjHandle(f);
    handle
  }
  
  override def read(handle : FileHandle, buffer : ByteBuffer, offset : Long) : Int = {
    val file : File = handle.getObjHandle().asInstanceOf[File]
    var limit : Int = buffer.limit()
    
    
    if (file.content.length - offset < limit)
      limit = (file.content.length - offset).asInstanceOf[Int]
    
    buffer.put(file.content, offset.asInstanceOf[Int], limit)
    limit
  }
  
  def getFile(path : String) : File = {
    var found : File = null;
    staticFiles.foreach(file => 
      {
        if (file.name == path)
          found = file;
      }
    )
    if (found == null)
      throw new PathNotFoundException(path)
    
    found
  }
  
  override def close(handle : FileHandle) = {
  }

  override def createDirectory(path : String) = {
    if (path == "/")
      throw new DestinationAlreadyExistsException()
    
    throw new AccessDeniedException()
  }

  override def createFile(path : String) = {
    throw new AccessDeniedException()
  }
  
  override def flush(handle : FileHandle) = {
  }

  override def setLength(handle : FileHandle, length : Long) = {
  }

  override def write(handle : FileHandle, buffer : ByteBuffer, offset : Long) = {
  }
}