package org.xnat.xnatfs;

import fuse.*;
import java.util.*;
import org.apache.log4j.*;

import net.sf.ehcache.*;

/**
 * Represents an XNAT project. Contains users, members, owners, collaborators,
 * and subjects.
 * 
 * @author blezek
 * 
 * @see Users
 * @see Members
 * @see Collaborators
 * @see Subjects
 * @see Projects
 */
public class Project extends Node {

  private static final Logger logger = Logger.getLogger ( Users.class );
  String mProjectId;

  static ArrayList<String> StaticChildren;
  static {
    StaticChildren = new ArrayList<String> ();
    StaticChildren.add ( "prearchive_code" );
    StaticChildren.add ( "quarantine_code" );
    StaticChildren.add ( "current_arc" );
  }

  /**
   * Construct a Project with the given path and ProjectID.
   * 
   * @param path
   *          Virtual path of this project.
   * @param projectid
   *          Project id
   */
  public Project ( String path, String projectid ) {
    super ( path );
    mProjectId = projectid;
  }

  /**
   * Get attribute.
   * 
   * @see org.org.xnat.xnatfs.webdav.Node#getattr(java.lang.String, fuse.FuseGetattrSetter)
   */
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

  /**
   * Get the contents of the directory, filling in the <code>filler</code>.
   * Contains users, members, owners, collaborators and subjects.
   * 
   * @see org.org.xnat.xnatfs.webdav.Node#getdir(java.lang.String, fuse.FuseDirFiller)
   */
  @Override
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      for ( String child : StaticChildren ) {
        filler.add ( child, child.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      for ( String ext : RemoteFile.sExtensions ) {
        filler.add ( "project" + ext, ext.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      filler.add ( "users", "users".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "members", "members".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "owners", "owners".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "collaborators", "collaborators".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
      filler.add ( "subjects", "subjects".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
      return 0;
    }
    return Errno.ENOTDIR;
  }

  /**
   * Create a child, if possible.
   * 
   * @see Project
   * @see RemoteFile
   * @see Subjects
   * @see Users
   * @see org.org.xnat.xnatfs.webdav.Node#createChild(java.lang.String)
   */
  @Override
  public Node createChild ( String child ) {
    String childPath = mPath + "/" + child;
    if ( StaticChildren.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.startsWith ( "project" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "users" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new Users ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "members" ) || child.equals ( "collaborators" ) || child.equals ( "owners" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      String url = mPath + "/users/" + child;
      Element element = new Element ( childPath, new Users ( childPath, url ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "subjects" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new Subjects ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return null;
  }

}
