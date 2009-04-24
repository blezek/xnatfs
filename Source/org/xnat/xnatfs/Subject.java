

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
public class Subject extends Node {

  private static final Logger logger = Logger.getLogger(Users.class);
  String mSubjectId;

  public Subject ( String path, String subjectid ) {
    super ( path );
    mSubjectId = subjectid;
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      setter.set(
                 this.hashCode(),
                 FuseFtypeConstants.TYPE_DIR | 0755,
                 0,
                 0, 0, 0,
                 -1, -1,
                 time, time, time
                 );
      return 0;
    } 
    return Errno.ENOENT;
  }
  
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    return 0;
    /*
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      for ( String child : StaticChildren ) {
        filler.add ( child, child.hashCode(), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      filler.add ( "users", "users".hashCode(), FuseFtypeConstants.TYPE_DIR | 0555 );
      return 0;
    }
    return Errno.ENOTDIR;
    */
  }
  /** Create a child of this node.  Note, the child is a single filename, not a path
   */
  public Node createChild ( String child ) {
    return null;
  }
         
}
