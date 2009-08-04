package org.xnat.xnatfs;

import fuse.*;

import java.util.*;
import org.apache.log4j.*;
import net.sf.ehcache.*;

/**
 * Class to handle a users. Shows up as a directory with three files in it.
 */
public class Scans extends Container {

  private static final Logger logger = Logger.getLogger ( Scans.class );

  public Scans ( String path ) {
    super ( path );
  }

  public Scans ( String path, String childkey ) {
    super ( path );
    mChildKey = childkey;
  }

  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  @Override
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      return super.getdir ( path, filler );
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
    HashSet<String> experimentList = getElementList ();
    if ( experimentList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Scan ( childPath, child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return super.createChild ( child );
  }

}
