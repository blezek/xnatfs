package org.xnat.xnatfs;

import fuse.*;
import org.apache.log4j.*;

import net.sf.ehcache.*;

/**
 * Root node, knows how to create sub-directories.
 */
public class Root extends Node {

  private static final Logger logger = Logger.getLogger ( Root.class );

  public Root ( String path ) {
    super ( path );
  }

  /**
   * Get attributes of this Node. The path is guarenteed to match our path.
   */
  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1 * xnatfs.NAME_LENGTH, ( 1 * xnatfs.NAME_LENGTH + xnatfs.BLOCK_SIZE - 1 ) / xnatfs.BLOCK_SIZE, xnatfs.sTimeStamp,
          xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  @Override
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    if ( path.equals ( mPath ) ) {
      filler.add ( "users", 1, FuseFtypeConstants.TYPE_DIR | 0555 );
      createChild ( "users" );
      filler.add ( "projects", 1, FuseFtypeConstants.TYPE_DIR | 0555 );
      createChild ( "projects" );
      // Stop Spotlight from indexing the drive
      filler.add ( ".metadata_never_index", 1, FuseFtypeConstants.TYPE_FILE | 0444 );
      createChild ( ".metadata_never_index" );
      return 0;
    }
    return Errno.ENOTDIR;
  }

  @Override
  public Node createChild ( String child ) {
    if ( child.equals ( "users" ) ) {
      logger.debug ( "Creating: " + child + " in: " + mPath + child );
      Element element = new Element ( mPath + child, new Users ( mPath + child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "projects" ) ) {
      logger.debug ( "Creating: " + child + " in: " + mPath + child );
      Element element = new Element ( mPath + child, new Projects ( mPath + child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( ".metadata_never_index" ) ) {
      logger.debug ( "Creating: " + child + " in: " + mPath + child );
      Element element = new Element ( mPath + child, new EmptyFile ( mPath + child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    logger.debug ( "Unrecognized child" );
    return null;
  }

}
