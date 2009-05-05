



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

public class RemoteListFile extends Node {
  public static HashSet<String> sExtensions;
  private static final Logger logger = Logger.getLogger(RemoteListFile.class);
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

  void setSize ( long s ) { mSize = s; }

  long getSize () throws Exception {
    // If it was cached, just return
    if ( mSize == -1 ) {
      mSize = getContents().length;
    }
    return mSize;
  }

  byte[] getContents () throws Exception {
    // Get and/or cache this files contents
    Element n = xnatfs.sContentCache.get ( mPath );
    if ( n != null ) {
      return (byte[]) n.getObjectValue();
    }
    String e = mPath;
    if ( mUrl != null ) {
      e = mUrl;
    }
    if ( mFormat != null ) {
      if ( e.endsWith( mFormat ) ) {
        e = e.replaceAll( mFormat, "?format=" + mFormat.substring(1) );
      }
      logger.debug ( "Fetching path: " + e );
    }
    byte[] content = XNATConnection.getInstance().getURLAsBytes ( e );
    n = new Element ( mPath, content );
    xnatfs.sContentCache.put ( n );
    return content;
  }

  public RemoteListFile ( String path, String format ) {
    super ( path );
    mFormat = format;
    logger.debug ( "Created " + path + " format: " + format );
  }

  public RemoteListFile ( String path ) {
    super ( path );
    mFormat = null;
  }
  /** Put in the file system as path, but fetch from url */
  public RemoteListFile ( String path, String format, String url ) {
    this ( path, format );
    mUrl = url;
    logger.debug( "Url: " + mUrl );
  }
    

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      try {
        // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long size, long blocks, int atime, int mtime, int ctime) 
        setter.set(
                   this.hashCode(),
                   FuseFtypeConstants.TYPE_FILE | 0444,
                   0,
                   0, 0, 0,
                   getSize(),
                   (getSize() + xnatfs.BLOCK_SIZE - 1) / xnatfs.BLOCK_SIZE,
                   time, time, time
                   );
        return 0;
      } catch ( Exception e ) {
        throw new FuseException( "Failed to get size of object", e );
      }
    }
    return Errno.ENOENT;
  }

  public int open ( String path, int flags, FuseOpenSetter openSetter ) throws FuseException { 
    openSetter.setFh ( this );
    return 0; 
  };

  // Open, etc.
  public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
    if ( path.equals ( mPath ) ) {
      try {
        // get the file from XNAT
        byte[] content = getContents();
        buf.put(content, (int) offset, Math.min(buf.remaining(), content.length - (int)offset));
        return 0;
      } catch ( Exception e ) {
        throw new FuseException();
      }
    }
    return Errno.EBADF;
  }

  public int flush(String path, Object fh) throws FuseException {
    return 0;
  }
  public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
    return 0;
  }
  public int release(String path, Object fh, int flags) throws FuseException {
    return 0;
  }
}
