

package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;
import java.util.*;
import org.apache.log4j.*;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;
/**
 * Root node, knows how to create sub-directories.
 */
public class Root extends Node {

  private static final Logger logger = Logger.getLogger(Root.class);
  public Root ( String path ) {
    super ( path );
  }

  /** Get attributes of this Node.  The path is guarenteed to match our path.
   */
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      setter.set(
                 this.hashCode(),
                 FuseFtypeConstants.TYPE_DIR | 0755,
                 1,
                 0, 0,
                 0,
                 1 * xnatfs.NAME_LENGTH,
                 (1 * xnatfs.NAME_LENGTH + xnatfs.BLOCK_SIZE - 1) / xnatfs.BLOCK_SIZE,
                 time, time, time
                 );
      return 0;
    } 
    return Errno.ENOENT;
  }
    
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    if ( path.equals ( mPath ) ) {
      filler.add ( "users",
                   1,
                   FuseFtypeConstants.TYPE_DIR | 0555 );
      return 0;
    }
    return Errno.ENOTDIR;
  }
    
  public Node createChild ( String child ) {
    if ( child.equals ( "users" ) ) {
      logger.debug ( "Creating: " + child );
      Element element = new Element ( mPath + child, new Users ( mPath + child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    logger.debug ( "Unrecognized child" );
    return null;
  }
         
}
