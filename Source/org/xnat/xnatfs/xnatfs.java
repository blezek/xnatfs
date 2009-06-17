package org.xnat.xnatfs;

import org.apache.log4j.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.util.*;

import org.json.*;

// import org.jdom.*;
// import org.jdom.input.*;
import java.io.IOException;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fuse.*;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;

@SuppressWarnings( { "OctalInteger" })
public class xnatfs implements Filesystem3, LifecycleSupport {
  private static final Logger logger = Logger.getLogger ( xnatfs.class );

  public static Cache sNodeCache;
  public static Cache sContentCache;
  public static Cache sFileHandleCache;
  public static CacheManager mMemoryCacheManager;
  public static ExecutorService sExecutor = Executors.newCachedThreadPool ();
  public static File sTemporaryDirectory;

  static {
    File props = new File ( "log4j.properties" );
    if ( props.exists () && props.canRead () ) {
      PropertyConfigurator.configure ( props.getAbsolutePath () );
    } else {
      BasicConfigurator.configure ();
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

  public int getattr ( String path, FuseGetattrSetter getattrSetter ) throws FuseException {
    logger.info ( "getattr: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.getattr ( path, getattrSetter );
    }
    return Errno.ENOENT;
  }

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
    return 0;
  }

  public int chown ( String path, int uid, int gid ) throws FuseException {
    logger.info ( "chown: " + path );
    return 0;
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

  public int statfs ( FuseStatfsSetter statfsSetter ) throws FuseException {
    logger.info ( "statfs" );
    // set(int blockSize, int blocks, int blocksFree, int blocksAvail, int files, int filesFree, int namelen)
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
    return 0;
  }

  // Read the correct name of a linked file
  public int readlink ( String path, CharBuffer link ) throws FuseException {
    return Errno.ENOENT;
  }

  // if open returns a filehandle by calling FuseOpenSetter.setFh() method, it will be passed to every method that supports 'fh' argument
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

  // fh is filehandle passed from open,
  // isWritepage indicates that write was caused by a writepage
  public int write ( String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset ) throws FuseException {
    logger.info ( "write: " + path );
    return Errno.EROFS;
  }

  // fh is filehandle passed from open
  public int read ( String path, Object fh, ByteBuffer buf, long offset ) throws FuseException {
    logger.info ( "read: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.read ( path, fh, buf, offset );
    }
    return Errno.ENOENT;
  }

  // new operation (called on every filehandle close), fh is filehandle passed from open
  public int flush ( String path, Object fh ) throws FuseException {
    logger.info ( "flush: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.flush ( path, fh );
    }
    return Errno.ENOENT;
  }

  // new operation (Synchronize file contents), fh is filehandle passed from open,
  // isDatasync indicates that only the user data should be flushed, not the meta data
  public int fsync ( String path, Object fh, boolean isDatasync ) throws FuseException {
    logger.info ( "fsync: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.fsync ( path, fh, isDatasync );
    }
    return Errno.EBADF;
  }

  // (called when last filehandle is closed), fh is filehandle passed from open
  public int release ( String path, Object fh, int flags ) throws FuseException {
    logger.info ( "release: " + path );
    Node node = Dispatcher.getNode ( path );
    if ( node != null ) {
      return node.release ( path, fh, flags );
    }
    return Errno.EBADF;
  }

  //
  // LifeCycleSupport
  public int init () {
    logger.info ( "Initializing Filesystem" );
    return 0;
  }

  public int destroy () {
    logger.info ( "Destroying Filesystem" );
    return 0;
  }

  static public void configureCache () {
    URL url = ClassLoader.getSystemResource ( "ehcache.xml" );
    logger.info ( "Found configuration URL: " + url );
    if ( url != null ) {
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

  static public void configureConnection () {
    XNATConnection.getInstance ().setUsername ( "blezek" );
    XNATConnection.getInstance ().setPassword ( "throwaway" );
  }

  //
  // Java entry point
  public static void main ( String[] args ) {
    Logger.getLogger ( "org.xnat.xnatfs" ).setLevel ( Level.DEBUG );
    Logger.getLogger ( "org.apache.commons" ).setLevel ( Level.WARN );
    Logger.getLogger ( "httpclient.wire" ).setLevel ( Level.WARN );
    Logger.getLogger ( "org.apache.http" ).setLevel ( Level.WARN );
    logger.info ( "Starting xnatfs" );
    configureCache ();
    configureConnection ();

    try {
      Log l = LogFactory.getLog ( "org.xnat.xnatfs.FuseMount" );
      FuseMount.mount ( args, new xnatfs (), l );
    } catch ( Exception e ) {
      e.printStackTrace ();
    } finally {
      logger.info ( "exiting" );
      CacheManager.getInstance ().shutdown ();
      sExecutor.shutdownNow ();
      logger.info ( "exited" );
    }
  }
}
