package org.xnat.xnatfs;

import java.util.ArrayList;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import fuse.Errno;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtypeConstants;
import fuse.FuseGetattrSetter;

/**
 * Class to handle a users. Shows up as a directory with three files in it.
 */
public class Users extends Node {

  private static final Logger logger = Logger.getLogger ( Users.class );
  ArrayList<String> mChildTypes = new ArrayList<String> ();
  String mUrl;

  public Users ( String path ) {
    this ( path, path );
  }

  public Users ( String path, String url ) {
    super ( path );
    mUrl = url;
    mChildTypes.add ( tail ( path ) );
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis () / 1000L);
    if ( path.equals ( mPath ) ) {
      // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long
      // size, long blocks, int atime, int mtime, int ctime)
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, time, time, time );
      return 0;
    }
    return Errno.ENOENT;
  }

  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      for ( String base : mChildTypes ) {
        for ( String e : RemoteFile.sExtensions ) {
          createChild ( base + e );
          filler.add ( base + e, (base + e).hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
        }
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  /**
   * Create a child of this node. Note, the child is a single filename, not a
   * path
   */
  public Node createChild ( String child ) {
    if ( RemoteFile.sExtensions.contains ( extention ( child ) ) && mChildTypes.contains ( root ( child ) ) ) {
      // See if it exists in the cache
      String path = mPath + "/" + child;
      String url = path;
      if ( mUrl != null ) {
        url = mUrl + "/" + child;
      }
      logger.debug ( "Created child " + path + " " + url );
      Element element = new Element ( path, new RemoteFile ( path, extention ( child ), url ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    } else {
      logger.debug ( "Couldn't find extention: " + extention ( child ) );
    }
    return null;
  }

}
