

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
public class Project extends Node {

  private static final Logger logger = Logger.getLogger(Users.class);
  String mProjectId;

  static ArrayList<String> StaticChildren;
  static {
    StaticChildren = new ArrayList<String> ();
    StaticChildren.add ( "prearchive_code" ); StaticChildren.add ( "quarantine_code" ); StaticChildren.add ( "current_arc" );
  }


  public Project ( String path, String projectid ) {
    super ( path );
    mProjectId = projectid;
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long size, long blocks, int atime, int mtime, int ctime) 
      setter.set(
                 this.hashCode(),
                 FuseFtypeConstants.TYPE_DIR | 0755,
                 0,
                 0, 0, 0,
                 1, 1,
                 time, time, time
                 );
      return 0;
    } 
    return Errno.ENOENT;
  }
  
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      for ( String child : StaticChildren ) {
        filler.add ( child, child.hashCode(), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      for ( String ext : RemoteListFile.sExtensions ) {
        filler.add ( "project" + ext, ext.hashCode(), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      filler.add ( "users", "users".hashCode(), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "members", "members".hashCode(), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "owners", "owners".hashCode(), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "collaborators", "collaborators".hashCode(), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "subjects", "subjects".hashCode(), FuseFtypeConstants.TYPE_DIR | 0555 );
      return 0;
    }
    return Errno.ENOTDIR;
  }
  /** Create a child of this node.  Note, the child is a single filename, not a path
   */
  public Node createChild ( String child ) {
    String childPath = mPath + "/" + child;
    if ( StaticChildren.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new RemoteListFile ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    if ( child.startsWith ( "project" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new RemoteListFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
      }
    if ( child.equals ( "users" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new Users ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    if ( child.equals ( "members" )
         || child.equals ( "collaborators" )
         || child.equals ( "owners" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      String url = mPath + "/users/" + child;
      Element element = new Element ( childPath, new Users ( childPath, url ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    if ( child.equals ( "subjects" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new Subjects ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    return null;
  }
         
}
