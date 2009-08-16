package org.xnat.xnatfs;

import fuse.*;

import org.apache.log4j.*;

import net.sf.ehcache.*;

/**
 * Experiment object. Represents an XNAT Experiment, contains several files and
 * a scans directory.
 * 
 * @author blezek
 * 
 */
public class Experiment extends Node {

  private static final Logger logger = Logger.getLogger ( Experiment.class );
  String mExperimentId;

  /**
   * Construct an Experiment with the given path and id.
   * 
   * @param path
   *          Virtual path.
   * @param experimentid
   *          Unique id.
   */
  public Experiment ( String path, String experimentid ) {
    super ( path );
    mExperimentId = experimentid;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#getattr(java.lang.String, fuse.FuseGetattrSetter)
   */
  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 0, 0, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  /*
   * Get the directory contents.
   * 
   * @see org.xnat.xnatfs.Node#getdir(java.lang.String, fuse.FuseDirFiller)
   */
  @Override
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    filler.add ( "experiment.xml", "experiment.xml".hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
    filler.add ( "status", "status".hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
    filler.add ( "assessors", "assessors".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
    filler.add ( "scans", "scans".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
    filler.add ( "resources", "resources".hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
    String base = "projects";
    for ( String e : RemoteFile.sExtensions ) {
      createChild ( base + e );
      filler.add ( base + e, ( base + e ).hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
    }
    return 0;
  }

  /**
   * Create the specified child. Creates experiment.xml, status, assessors and
   * scans.
   * 
   * @see Scans
   * @see org.org.xnat.xnatfs.webdav.Node#createChild(java.lang.String)
   */
  @Override
  public Node createChild ( String child ) {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath );
    if ( child.equals ( "experiment.xml" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "status" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "assessors" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Assessors ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "scans" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Scans ( childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.equals ( "resources" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Scans ( childPath, "label" ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    if ( child.startsWith ( "projects" ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, extention ( child ), childPath ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return null;
  }

}
