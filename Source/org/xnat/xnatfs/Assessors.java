package org.xnat.xnatfs;

import fuse.*;

import java.util.*;
import org.apache.log4j.*;
import net.sf.ehcache.*;

/**
 * Class to handle a users. Shows up as a directory with three files in it.
 */
public class Assessors extends Container {

  private static final Logger logger = Logger.getLogger ( Assessors.class );

  public Assessors ( String path ) {
    super ( path );
  }

  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long
      // size, long blocks, int atime, int mtime, int ctime)
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  @Override
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      HashSet<String> assessorList = getElementList ();
      for ( String assessor : assessorList ) {
        createChild ( assessor + ".xml" );
        filler.add ( assessor + ".xml", assessor.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      String t = tail ( mPath );
      for ( String extention : RemoteFile.sExtensions ) {
        String c = t + extention;
        filler.add ( c, c.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
        // String base = "projects";
        // createChild ( base + extention );
        // filler.add ( base + extention, (base + extention).hashCode (),
        // FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  /**
   * Create a child of this node. Note, the child is a single filename, not a
   * path
   */
  @Override
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath );
    HashSet<String> assessorList = getElementList ();
    if ( assessorList.contains ( root ( child ) ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, ".xml" ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.startsWith ( "projects" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, extention ( child ), childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return super.createChild ( child );
  }

}
