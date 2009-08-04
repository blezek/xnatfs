package org.xnat.xnatfs;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import fuse.Errno;
import fuse.FuseException;
import fuse.FuseFtypeConstants;
import fuse.FuseGetattrSetter;
import fuse.FuseOpenSetter;

public class EmptyFile extends Node {
  private static final Logger logger = Logger.getLogger ( EmptyFile.class );

  public EmptyFile ( String path ) {
    super ( path );
    logger.debug ( "Created " + path );
  }

  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    if ( path.equals ( mPath ) ) {
      // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long
      // size, long blocks, int atime, int mtime, int ctime)
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444, 0, 0, 0, 0, 0, 0, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  @Override
  public int open ( String path, int flags, FuseOpenSetter openSetter ) throws FuseException {
    openSetter.setFh ( this );
    return 0;
  };

  // Open, etc.
  @Override
  public int read ( String path, Object fh, ByteBuffer buf, long offset ) throws FuseException {
    return Errno.EBADF;
  }

  @Override
  public int flush ( String path, Object fh ) throws FuseException {
    return 0;
  }

  @Override
  public int fsync ( String path, Object fh, boolean isDatasync ) throws FuseException {
    return 0;
  }

  @Override
  public int release ( String path, Object fh, int flags ) throws FuseException {
    return 0;
  }
}
