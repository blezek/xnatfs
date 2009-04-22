

package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;
import java.util.*;
import org.apache.log4j.*;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;
/**
 * Class to handle a users.  Shows up as a directory with three files in it.
 */
public class Users extends Node {

  private static final Logger logger = Logger.getLogger(Users.class);
  public Users ( String path ) {
    super ( path );
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      setter.set(
                        this.hashCode(),
                        FuseFtypeConstants.TYPE_DIR | 0755,
                        3,
                        0, 0, 0,
                        -1, -1,
                        time, time, time
                        );
      return 0;
    } 
    return Errno.ENOENT;
  }
    
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      for ( String e : RemoteListFile.sExtensions ) {
        createChild ( "users" + e );
        filler.add ( "users" + e,
                     e.hashCode(),
                     FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }
  /** Create a child of this node.  Note, the child is a single filename, not a path
   */
  public Node createChild ( String child ) {
      if ( RemoteListFile.sExtensions.contains ( extention ( child ) ) ) {
        // See if it exists in the cache
    	String path = mPath + "/" + child;
        Element element = new Element ( path, new RemoteListFile ( path ) );
        xnatfs.sNodeCache.put ( element );
        return (Node)element.getObjectValue();
      }
    return null;
  }
         
}
