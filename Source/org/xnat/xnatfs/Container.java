package org.xnat.xnatfs;

import java.io.InputStreamReader;
import java.util.HashSet;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import fuse.Errno;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtypeConstants;

public abstract class Container extends Node {
  private static final Logger logger = Logger.getLogger(Container.class);
  String mChildKey = "id";

  public Container(String path) {
    super(path);
  }

  protected HashSet<String> getElementList(String field) throws FuseException {
    // Get the subjects code
    Element element = xnatfs.sContentCache.get ( mPath );
    HashSet<String> list = null;
    if ( element == null ) {
      try {
        list = new HashSet<String>();
        InputStreamReader reader = new InputStreamReader ( XNATConnection.getInstance().getURLAsStream ( mPath + "?format=json" ) );
        JSONTokener tokenizer = new JSONTokener ( reader );
        JSONObject json = new JSONObject ( tokenizer );
        JSONArray subjects = json.getJSONObject ( "ResultSet" ).getJSONArray ( "Result" );
        logger.debug( "Found: " + subjects.length() + " elements" );
        for ( int idx = 0; idx < subjects.length(); idx++ ) {
          if ( subjects.isNull ( idx ) ) { continue; }
          String id = subjects.getJSONObject ( idx ).getString ( field );
          list.add ( id );
        }
      } catch ( Exception e ) {
        logger.error ( "Caught exception reading " + mPath, e );
        throw new FuseException();
      }
      element = new Element ( mPath, list );
      xnatfs.sContentCache.put ( element );
    }
    list = (HashSet<String>) element.getObjectValue();
    return list;
  }
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      HashSet<String> projectList = getElementList( mChildKey );
      for ( String project : projectList ) {
        createChild ( project );
        filler.add ( project,
                     project.hashCode(),
                     FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      String t = tail ( mPath );
      for ( String extention : RemoteListFile.sExtensions ) {
        String c = t+extention;
        filler.add ( c, c.hashCode(), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  public Node createChild ( String child ) throws FuseException {
    logger.debug ( "createChild: " + child + " in path " + mPath );
    if ( child.startsWith ( tail ( mPath ) ) && RemoteListFile.sExtensions.contains ( extention ( child ) ) ) {
      logger.debug ( "Create child " + child + " of " + mPath );
      String childPath = mPath + "/" + child;
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new RemoteListFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    return null;
  }
      



    
}
