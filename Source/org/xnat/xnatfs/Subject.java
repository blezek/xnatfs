package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;

import java.util.*;
import org.apache.log4j.*;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;

/**
 * Class to handle a users. Shows up as a directory with three files in it.
 */
public class Subject extends Node {

  private static final Logger logger = Logger.getLogger ( Subject.class );
  String mSubjectId;

  public Subject ( String path, String subjectid ) {
    super ( path );
    mSubjectId = subjectid;
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 0, 0, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    filler.add ( "subject.xml", "subject.xml".hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
    filler.add ( "experiments", "experiments".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
    String base = "projects";
    for ( String e : RemoteFile.sExtensions ) {
      createChild ( base + e );
      filler.add ( base + e, (base + e).hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
    }
    return 0;
  }

  /**
   * Create a child of this node. Note, the child is a single filename, not a
   * path
   */
  public Node createChild ( String child ) {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath );
    if ( child.startsWith ( "subject" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
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
    if ( child.equals ( "experiments" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new Experiments ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }

    return null;
  }
}
