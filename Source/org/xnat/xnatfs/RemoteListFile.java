



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
  static {
    sExtensions = new HashSet<String> ( 4 ); 
    sExtensions.add ( ".csv" );
    sExtensions.add ( ".html" );
    sExtensions.add ( ".xml" );
    sExtensions.add ( ".json" );
  }

  byte[] getContents () throws Exception {
    // Get and/or cache this files contents
    Element n = xnatfs.sContentCache.get ( mPath );
    if ( n != null ) {
      return (byte[]) n.getObjectValue();
    }
    String e = mPath;
    if ( mFormat != null ) {
      if ( mPath.endsWith( mFormat ) ) {
        e = mPath.replaceAll( "." + mFormat, "?format=" + mFormat );
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
  }
  public RemoteListFile ( String path ) {
    super ( path );
    mFormat = null;
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      byte[] content = null;
      try {
    	  content = getContents();
      } catch ( Exception e ) {
    	  logger.error ( "Error fetching contents", e );
    	  throw new FuseException ();
      }
      setter.set(
                        this.hashCode(),
                        FuseFtypeConstants.TYPE_FILE | 0444,
                        1,
                        0, 0, 0,
                        content.length,
                        (content.length + xnatfs.BLOCK_SIZE - 1) / xnatfs.BLOCK_SIZE,
                        time, time, time
                        );
      return 0;
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
