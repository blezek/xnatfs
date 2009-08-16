package org.xnat.xnatfs;

import org.apache.log4j.*;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import fuse.*;

import net.sf.ehcache.*;

public class xnatfs implements Filesystem3, XattrSupport, LifecycleSupport {
  private static final Logger logger = Logger.getLogger ( xnatfs.class );

  /** Cache services from ehcache */
  public static Cache sNodeCache;
  public static Cache sContentCache;
  public static Cache sFileHandleCache;
  public static CacheManager mMemoryCacheManager;

  /** Thread pool for background downloads of files */
  public static ExecutorService sExecutor = Executors.newCachedThreadPool ();
  public static File sTemporaryDirectory;
  public static int sTimeStamp = (int) ( System.currentTimeMillis () / 1000L );

  static {
    // First try to configure from local directory, then from application
    // support directory
    File props;
    props = new File ( System.getProperty ( "user.dir", "." ), "log4j.properties" );
    if ( props.exists () && props.canRead () ) {
      PropertyConfigurator.configure ( props.getAbsolutePath () );
    } else {
      props = new File ( Start.getApplicationResourceDirectory ( "xnatfs" ), "log4j.properties" );
      if ( props.exists () && props.canRead () ) {
        PropertyConfigurator.configure ( props.getAbsolutePath () );
      } else {
        BasicConfigurator.configure ();
      }
    }

  }

  class PathAndName {
    public String mPath;
    public String mName;

    public PathAndName ( String path, String name ) {
      mPath = path;
      mName = name;
    }
  }

  public static final int BLOCK_SIZE = 512;
  public static final int NAME_LENGTH = 1024;

  /**
   * Constructor. Initialized the cache, temporary directories and the Root
   * element.
   */
  public xnatfs () {
    configureCache ();
    configureConnection ();
    sTemporaryDirectory = new File ( System.getProperty ( "java.io.tmpdir" ) );
    if ( mMemoryCacheManager.getDiskStorePath () != null ) {
      sTemporaryDirectory = new File ( mMemoryCacheManager.getDiskStorePath () );
    }
    sTemporaryDirectory = new File ( sTemporaryDirectory, "FileCache" );
    sTemporaryDirectory.mkdirs ();
    Root root = new Root ( "/" );
    Element e = new Element ( "/", root );
    e.setEternal ( true );
    sNodeCache.put ( e );
  }

  /** Get attributes, delegated to the Node specified by the path */
  public int getattr ( String path, FuseGetattrSetter getattrSetter ) throws FuseException {
    logger.info ( "getattr: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.getattr ( path, getattrSetter );
    }
    return Errno.ENOENT;
  }

  /*
   * Get directory information for the path. Delegates through the Node.
   * 
   * @see fuse.Filesystem3#getdir(java.lang.String, fuse.FuseDirFiller)
   */
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.info ( "getdir: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.getdir ( path, filler );
    }
    return Errno.ENOTDIR;
  }

  public int chmod ( String path, int mode ) throws FuseException {
    logger.info ( "chmod: " + path );
    return Errno.ENOTSUPP;
  }

  public int chown ( String path, int uid, int gid ) throws FuseException {
    logger.info ( "chown: " + path );
    return Errno.ENOTSUPP;
  }

  public int link ( String from, String to ) throws FuseException {
    return Errno.EROFS;
  }

  public int mkdir ( String path, int mode ) throws FuseException {
    return Errno.EROFS;
  }

  public int mknod ( String path, int mode, int rdev ) throws FuseException {
    return Errno.EROFS;
  }

  public int rename ( String from, String to ) throws FuseException {
    return Errno.EROFS;
  }

  public int rmdir ( String path ) throws FuseException {
    return Errno.EROFS;
  }

  /*
   * Returns statics for the filesystem
   * 
   * @see fuse.Filesystem3#statfs(fuse.FuseStatfsSetter)
   */
  public int statfs ( FuseStatfsSetter statfsSetter ) throws FuseException {
    logger.info ( "statfs" );
    // set(int blockSize, int blocks, int blocksFree, int blocksAvail, int
    // files, int filesFree, int namelen)
    statfsSetter.set ( BLOCK_SIZE, 1000, 200, 180, 1, 0, NAME_LENGTH );
    return 0;
  }

  public int symlink ( String from, String to ) throws FuseException {
    return Errno.EROFS;
  }

  public int truncate ( String path, long size ) throws FuseException {
    return Errno.EROFS;
  }

  public int unlink ( String path ) throws FuseException {
    return Errno.EROFS;
  }

  public int utime ( String path, int atime, int mtime ) throws FuseException {
    return Errno.ENOTSUPP;
  }

  // Read the correct name of a linked file
  public int readlink ( String path, CharBuffer link ) throws FuseException {
    return Errno.ENOENT;
  }

  /*
   * Ff open returns a filehandle by calling FuseOpenSetter.setFh() method, it
   * will be passed to every method that supports 'fh' argument.
   * 
   * @see fuse.Filesystem3#open(java.lang.String, int, fuse.FuseOpenSetter)
   */
  public int open ( String path, int flags, FuseOpenSetter openSetter ) throws FuseException {
    try {
      logger.info ( "open: " + path );
      Node node = Dispatcher.getNode ( path );
      if ( node != null ) {
        return node.open ( path, flags, openSetter );
      }
    } catch ( Exception e ) {
      logger.error ( "Error opening " + path, e );
    }
    return Errno.ENOENT;
  }

  /*
   * fh is filehandle passed from open,isWritepage indicates that write was
   * caused by a writepage
   * 
   * @see fuse.Filesystem3#write(java.lang.String, java.lang.Object, boolean,
   * java.nio.ByteBuffer, long)
   */
  public int write ( String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset ) throws FuseException {
    logger.info ( "write: " + path );
    return Errno.EROFS;
  }

  /*
   * fh is filehandle passed from open
   * 
   * @see fuse.Filesystem3#read(java.lang.String, java.lang.Object,
   * java.nio.ByteBuffer, long)
   */
  public int read ( String path, Object fh, ByteBuffer buf, long offset ) throws FuseException {
    logger.info ( "read: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.read ( path, fh, buf, offset );
    }
    return Errno.ENOENT;
  }

  /*
   * new operation (called on every filehandle close), fh is filehandle passed
   * from open
   * 
   * @see fuse.Filesystem3#flush(java.lang.String, java.lang.Object)
   */
  public int flush ( String path, Object fh ) throws FuseException {
    logger.info ( "flush: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.flush ( path, fh );
    }
    return Errno.ENOENT;
  }

  /*
   * new operation (Synchronize file contents), fh is filehandle passed from
   * open, isDatasync indicates that only the user data should be flushed, not
   * the meta data
   * 
   * @see fuse.Filesystem3#fsync(java.lang.String, java.lang.Object, boolean)
   */
  public int fsync ( String path, Object fh, boolean isDatasync ) throws FuseException {
    logger.info ( "fsync: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.fsync ( path, fh, isDatasync );
    }
    return Errno.EBADF;
  }

  /*
   * (called when last filehandle is closed), fh is filehandle passed from open
   * 
   * @see fuse.Filesystem3#release(java.lang.String, java.lang.Object, int)
   */
  public int release ( String path, Object fh, int flags ) throws FuseException {
    logger.info ( "release: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.release ( path, fh, flags );
    }
    return Errno.EBADF;
  }

  /*
   * LifeCycleSupport
   * 
   * @see fuse.LifecycleSupport#init()
   */
  public int init () {
    logger.info ( "Initializing Filesystem" );
    return 0;
  }

  /*
   * Life cycle support
   * 
   * @see fuse.LifecycleSupport#destroy()
   */
  public int destroy () {
    logger.info ( "Destroying Filesystem" );
    return 0;
  }

  /*
   * Configure the cache by looking up the ehcache.xml file in the system
   * classpath. Expects to have a "Node", "Content" and "FileHandle" cache. If
   * any cache does not exist, it is created on the fly.
   * 
   * First checks the local directory, then the application directory, and
   * finally falls back on the embedded file.
   * 
   * Register a caches listener for the content cache to close and delete files.
   */
  static public void configureCache () {
    URL url = ClassLoader.getSystemResource ( "ehcache.xml" );
    logger.info ( "Found configuration URL: " + url );
    File SystemConfig = new File ( Start.getApplicationResourceDirectory ( "xnatfs" ), "ehcache.xml" );
    File LocalConfig = new File ( System.getProperty ( "user.dir", "." ), "ehcache.xml" );
    if ( LocalConfig.exists () && LocalConfig.canRead () ) {
      mMemoryCacheManager = CacheManager.create ( LocalConfig.getAbsolutePath () );
    } else if ( SystemConfig.exists () && SystemConfig.canRead () ) {
      mMemoryCacheManager = CacheManager.create ( SystemConfig.getAbsolutePath () );
    } else if ( url != null ) {
      mMemoryCacheManager = CacheManager.create ( url );
    } else {
      mMemoryCacheManager = CacheManager.create ();
    }

    if ( mMemoryCacheManager.getCache ( "Node" ) == null ) {
      mMemoryCacheManager.addCache ( "Node" );
    }
    if ( mMemoryCacheManager.getCache ( "Content" ) == null ) {
      mMemoryCacheManager.addCache ( "Content" );
    }
    sNodeCache = mMemoryCacheManager.getCache ( "Node" );
    sContentCache = mMemoryCacheManager.getCache ( "Content" );
    sContentCache.getCacheEventNotificationService ().registerListener ( new CacheFileCleanup () );
    sFileHandleCache = mMemoryCacheManager.getCache ( "FileHandle" );
    if ( sNodeCache == null ) {
      logger.error ( "Failed to create filecache" );
    }
  }

  /*
   * Configure the connection. Hard coded username and password for now.
   */
  static public void configureConnection () {
    XNATConnection.getInstance ().setUsername ( "blezek" );
    XNATConnection.getInstance ().setPassword ( "throwaway" );
  }

  /*
   * (non-Javadoc)
   * 
   * @see fuse.XattrSupport#getxattr(java.lang.String, java.lang.String,
   * java.nio.ByteBuffer)
   */
  /*
   * public int getxattr ( String path, String name, ByteBuffer dst ) throws
   * FuseException, BufferOverflowException { return Errno.ENOTSUPP; }
   * 
   * public int getxattr ( String path, String name, ByteBuffer dst, int
   * position ) throws FuseException, BufferOverflowException { logger.debug (
   * "getxattr called, returning ENOTSUPP " + Errno.ENOTSUPP ); return
   * Errno.ENOTSUPP; }
   */
  /*
   * (non-Javadoc)
   * 
   * @see fuse.XattrSupport#getxattrsize(java.lang.String, java.lang.String,
   * fuse.FuseSizeSetter)
   */
  public int getxattrsize ( String arg0, String arg1, FuseSizeSetter arg2 ) throws FuseException {
    arg2.setSize ( 0 );
    return 0;
    // 
    // return Errno.ENOTSUPP;
  }

  /*
   * (non-Javadoc)
   * 
   * @see fuse.XattrSupport#listxattr(java.lang.String, fuse.XattrLister)
   */
  public int listxattr ( String arg0, XattrLister arg1 ) throws FuseException {
    // TODO Auto-generated method stub
    return Errno.ENOTSUPP;
  }

  /*
   * (non-Javadoc)
   * 
   * @see fuse.XattrSupport#removexattr(java.lang.String, java.lang.String)
   */
  public int removexattr ( String arg0, String arg1 ) throws FuseException {
    // TODO Auto-generated method stub
    return Errno.ENOTSUPP;
  }

  /*
   * (non-Javadoc)
   * 
   * @see fuse.XattrSupport#setxattr(java.lang.String, java.lang.String,
   * java.nio.ByteBuffer, int)
   */
  public int setxattr ( String arg0, String arg1, ByteBuffer arg2, int arg3 ) throws FuseException {
    // TODO Auto-generated method stub
    return Errno.ENOTSUPP;
  }

  /*
   * (non-Javadoc)
   * 
   * @see fuse.XattrSupport#getxattr(java.lang.String, java.lang.String,
   * java.nio.ByteBuffer, int)
   */
  public int getxattr ( String path, String name, ByteBuffer dst, int position ) throws FuseException, BufferOverflowException {
    // TODO Auto-generated method stub
    // return Errno.ENOATTR;
    // return Errno.ENOTSUPP;
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see fuse.XattrSupport#setxattr(java.lang.String, java.lang.String,
   * java.nio.ByteBuffer, int, int)
   */
  public int setxattr ( String path, String name, ByteBuffer value, int flags, int position ) throws FuseException {
    // TODO Auto-generated method stub
    return Errno.ENOTSUPP;
  }
}
