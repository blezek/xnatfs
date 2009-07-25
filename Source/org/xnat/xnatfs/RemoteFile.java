package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.*;
import java.util.*;

import org.apache.log4j.Logger;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;

/**
 * RemoteFile is the representation of a remote XNAT resource, as a local file.
 * It supports methods for opening, reading, and closing files. If a file does
 * not have a pre-assigned size, it is downloaded when getSize() is requested.
 * 
 * xnatfs classes should use RemoteFile instances to represent individual files.
 * The sExtensions HashSet contains the list of common XNAT formats.
 * 
 * A RemoteFile has three important members: <code>mFormat</code>,
 * <code>mUrl</code> and <code>mPath</code>. <code>mPath</code> is inherited
 * from Node. <code>mPath</code> is the local filename, while <code>mUrl</code>
 * is the URL in XNAT. If not specified, <code>mFormat</code> is inferred from
 * the <code>mPath</code>. The URL is appended with a format specifier to
 * request from XNAT different representations of the data. The path of the
 * final URL is constructed as <code>mUrl</code>?format=<code>mFormat</code>.
 * 
 * @author blezek
 * 
 */
public class RemoteFile extends Node {
  /**
   * Contains known XNAT extensions.
   */
  public static HashSet<String> sExtensions;
  private static final Logger logger = Logger.getLogger ( RemoteFile.class );
  String mFormat = null;
  String mUrl = null;
  long mSize = -1;

  static {
    sExtensions = new HashSet<String> ( 4 );
    sExtensions.add ( ".csv" );
    sExtensions.add ( ".html" );
    sExtensions.add ( ".xml" );
    sExtensions.add ( ".json" );
  }

  /**
   * Set the size of the RemoteFile, if known.
   * 
   * @param s
   *          Size of the file
   */
  void setSize ( long s ) {
    mSize = s;
  }

  /**
   * Get the size of the RemoteFile. If set through the API, return, otherwise,
   * download the file and determine the file size.
   * 
   * @return Size if this file
   * @throws Exception
   */
  long getSize () throws Exception {
    // If it was cached, just return
    if ( mSize == -1 ) {
      FileHandle fh = null;
      try {
        fh = XNATConnection.getInstance ().getFileHandle ( getURL (), mPath );
        fh.open ();
        mSize = fh.waitForDownload ();
        fh.release ();
        logger.debug ( "Found length of " + mSize + " for " + mPath );
      } finally {
        fh.release ();
      }
    }
    return mSize;
  }

  /**
   * Get the Url of this virtual file. If the URL has been specified, return in
   * directly, otherwise, format based on the path and optional format
   * specification.
   * 
   * @return URL of this file.
   */
  String getURL () {
    String e = mPath;
    if ( mUrl != null ) {
      e = mUrl;
    }
    if ( mFormat != null ) {
      if ( e.endsWith ( mFormat ) ) {
        e = e.replaceAll ( mFormat, "?format=" + mFormat.substring ( 1 ) );
      }
      logger.debug ( "Fetching path: " + e );
    }
    return e;
  }

  /**
   * Construct a RemoteFile with the specified path and format. Constructs the
   * URL from the path.
   * 
   * @param path
   *          Local path
   * @param format
   *          Format to download from XNAT
   */
  public RemoteFile ( String path, String format ) {
    super ( path );
    mFormat = format;
    logger.debug ( "Created " + path + " format: " + format );
  }

  /**
   * Construct RemoteFile from the given path. Format is unspecified.
   * 
   * @param path
   *          Local pathname
   */
  public RemoteFile ( String path ) {
    super ( path );
    mFormat = null;
  }

  /**
   * Construct RemoteFile in path, with specified format and custom URL.
   * 
   * @param path
   *          Local pathname
   * @param format
   *          Format to download
   * @param url
   *          Specified URL (do not construct from path)
   */
  public RemoteFile ( String path, String format, String url ) {
    this ( path, format );
    mUrl = url;
    logger.debug ( "Url: " + mUrl );
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#getattr(java.lang.String, fuse.FuseGetattrSetter)
   */
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    if ( path.equals ( mPath ) ) {
      try {
        // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long
        // size, long blocks, int atime, int mtime, int ctime)
        setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444, 1, 0, 0, 0, getSize (), ( getSize () + xnatfs.BLOCK_SIZE - 1 ) / xnatfs.BLOCK_SIZE, xnatfs.sTimeStamp, xnatfs.sTimeStamp,
            xnatfs.sTimeStamp );
        return 0;
      } catch ( Exception e ) {
        throw new FuseException ( "Failed to get size of object", e );
      }
    }
    return Errno.ENOENT;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#open(java.lang.String, int, fuse.FuseOpenSetter)
   */
  public int open ( String path, int flags, FuseOpenSetter openSetter ) throws FuseException {
    logger.debug ( "open " + path );
    try {
      FileHandle fh = XNATConnection.getInstance ().getFileHandle ( getURL (), mPath );
      fh.open ();
      openSetter.setFh ( fh );
    } catch ( Exception ex ) {
      logger.error ( "Error creating remote file handle for " + getURL (), ex );
      throw new FuseException ( ex );
    }
    return 0;
  };

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#read(java.lang.String, java.lang.Object,
   * java.nio.ByteBuffer, long)
   */
  public int read ( String path, Object ifh, ByteBuffer buf, long offset ) throws FuseException {
    logger.debug ( "read " + path + " filehandle " + ifh + " buffer " + buf + " offset " + offset );
    FileHandle fh = (FileHandle) ifh;
    try {
      // fh.waitForDownload ();
      fh.read ( buf, offset );
    } catch ( Exception e ) {
      logger.error ( "Error putting bytes into buffer", e );
      throw new FuseException ();
    }
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#flush(java.lang.String, java.lang.Object)
   */
  public int flush ( String path, Object fh ) throws FuseException {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#fsync(java.lang.String, java.lang.Object,
   * boolean)
   */
  public int fsync ( String path, Object fh, boolean isDatasync ) throws FuseException {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#release(java.lang.String, java.lang.Object, int)
   */
  public int release ( String path, Object ifh, int flags ) throws FuseException {
    FileHandle fh = (FileHandle) ifh;
    try {
      fh.release ();
    } catch ( Exception e ) {
      logger.error ( "Error releasing filehandle ", e );
      throw new FuseException ( e.toString () );
    }
    return 0;
  }
}
