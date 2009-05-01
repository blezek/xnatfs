



package org.xnat.xnatfs;
import org.apache.log4j.Logger;

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

public class EmptyFile extends Node {
  private static final Logger logger = Logger.getLogger(EmptyFile.class);

  public EmptyFile ( String path ) {
    super ( path );
    logger.debug ( "Created " + path );
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long size, long blocks, int atime, int mtime, int ctime) 
      setter.set(
                 this.hashCode(),
                 FuseFtypeConstants.TYPE_FILE | 0444,
                 0,
                 0, 0, 0,
                 0,
                 0,
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
