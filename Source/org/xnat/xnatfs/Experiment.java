

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
public class Experiment extends Node {

  private static final Logger logger = Logger.getLogger(Experiment.class);
  String mExperimentId;

  public Experiment ( String path, String experimentid ) {
    super ( path );
    mExperimentId = experimentid;
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
                 0, 0,
                 time, time, time
                 );
      return 0;
    } 
    return Errno.ENOENT;
  }
  
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    filler.add ( "experiment.xml", "experiment.xml".hashCode(), FuseFtypeConstants.TYPE_FILE | 0444 );
    filler.add ( "status", "status".hashCode(), FuseFtypeConstants.TYPE_FILE | 0444 );
    filler.add ( "assessors", "assessors".hashCode(), FuseFtypeConstants.TYPE_DIR | 0555 );
    return 0;
  }
  /** Create a child of this node.  Note, the child is a single filename, not a path
   */
  public Node createChild ( String child ) {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath  );
    if ( child.equals ( "experiment.xml" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new RemoteListFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    if ( child.equals ( "status" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new RemoteListFile ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    if ( child.equals ( "assessors" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new Assessors ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    return null;
  }
         
}
