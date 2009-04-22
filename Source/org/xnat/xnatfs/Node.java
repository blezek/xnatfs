/** Base class for any nodes in the REST api
 */

package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;

import java.util.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.*;

/** Abstract interface for a "directory" or "file" in the xnatfs.
 * Must be Serializable for use with the Ehcache manager.
 */
public abstract class Node implements Serializable {
  protected String mPath;
  protected String mName;

  public Node(String path) {
    this.mPath = path;
  }
  public abstract int getattr ( String path, FuseGetattrSetter setter ) throws FuseException;
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException { return Errno.ENOTDIR; };

  // file specific things
  public int open ( String path, int flags, FuseOpenSetter openSetter ) throws FuseException { return Errno.EBADF; };
  public int read ( String path, Object fh, ByteBuffer buf, long offset ) throws FuseException { return Errno.EBADF; };
  /// Called at file close
  public int flush ( String path, Object fh ) throws FuseException { return Errno.EBADF; };
  // (called when last filehandle is closed), fh is filehandle passed from open
  public int release ( String path, Object fh, int flags ) throws FuseException { return Errno.EBADF; };
  // new operation (Synchronize file contents), fh is filehandle passed from open,
  // isDatasync indicates that only the user data should be flushed, not the meta data
  public int fsync ( String path, Object fh, boolean isDatasync ) throws FuseException { return Errno.EBADF; };

  public Node createChild ( String childpath ) throws FuseException { return null; };
  
  // public API
  public synchronized String getPath() {
    return mPath;
  }

  public synchronized void setPath(String name) {
    this.mPath = name;
  }
  /** Get the root of the path, i.e. everything up to the last "/"
   */
  static public String root ( String path ) {
    int idx = path.lastIndexOf ( "/" );
    if ( idx == 0 ) { return "/"; }
    return path.substring ( 0, idx );
  }
  /** Get the tail of the path, i.e. everything past the last "/"
   */
  static public String tail ( String path ) {
    int idx = path.lastIndexOf("/");
    if ( idx < 0 ) { return ""; }
    return path.substring ( idx + 1 );
  }
  /** Get the extention of the path, i.e. everything past the last "."
   */
  static public String extention ( String path ) {
    return path.substring ( path.lastIndexOf ( "." ) );
  }
  
}
