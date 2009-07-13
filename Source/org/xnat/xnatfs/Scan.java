package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.log4j.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;

/**
 * Class to handle a users. Shows up as a directory with three files in it.
 */
public class Scan extends Container {

  private static final Logger logger = Logger.getLogger ( Scan.class );
  String mScanId;

  public Scan ( String path, String projectid ) {
    super ( path );
    mScanId = projectid;
    mChildKey = "label";
    logger.debug ( "Created scan with path: " + mPath );
  }

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

  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      filler.add ( "scan.xml", "scan.xml".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
      HashSet<String> resources = getElementList ( mPath + "/resources" );
      for ( String resource : resources ) {
        logger.debug ( "Creating child " + resource );
        createChild ( resource );
        filler.add ( resource, resource.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  /**
   * Create a child of this node. Note, the child is a single filename, not a
   * path
   */
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    if ( child.equals ( "scan.xml" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    HashSet<String> experimentList = getElementList ( mPath + "/resources" );
    if ( experimentList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Files ( childPath, mPath + "/resources/" + child + "/files" ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }

    return null;
  }

}
