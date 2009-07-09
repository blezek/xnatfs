package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;

import java.io.ByteArrayInputStream;
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
public class Files extends Container {

  private static final Logger logger = Logger.getLogger ( Files.class );

  public Files ( String path ) {
    super ( path );
    mChildKey = "Name";
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    String t = tail ( mPath );
    for ( String extention : RemoteFile.sExtensions ) {
      String c = t + extention;
      filler.add ( c, c.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
    }
    if ( path.equals ( mPath ) ) {
      HashMap<String, ArrayList<String>> map = getFileMap ();
      for ( String file : map.keySet () ) {
        createChild ( file );
        filler.add ( file, file.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
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
    logger.debug ( "Create child: " + child + " w/path: " + childPath );
    HashMap<String, ArrayList<String>> map = getFileMap ();
    if ( map.containsKey ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      String uri = map.get ( child ).get ( 0 ).replaceFirst ( "/REST", "" );
      long size = Long.valueOf ( map.get ( child ).get ( 1 ) ).longValue ();
      RemoteFile r = new RemoteFile ( childPath, null, uri );
      r.setSize ( size );
      Element element = new Element ( childPath, r );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return super.createChild ( child );
  }

  protected HashMap<String, ArrayList<String>> getFileMap () throws FuseException {
    ArrayList<String> l = new ArrayList<String> ();
    l.add ( "URI" );
    l.add ( "Size" );
    return getFileMap ( "Name", l );
  }

  protected synchronized HashMap<String, ArrayList<String>> getFileMap ( String keyField, ArrayList<String> valueFields ) throws FuseException {
    // Get the subjects code
    Element element = xnatfs.sContentCache.get ( mPath + "-HashMap" );
    HashMap<String, ArrayList<String>> map = null;
    if ( element == null ) {
      RemoteFileHandle fh = null;
      try {
        fh = XNATConnection.getInstance ().get ( mPath + "?format=json", mPath );
        map = new HashMap<String, ArrayList<String>> ();
        fh.waitForDownload ();
        InputStreamReader reader = new InputStreamReader ( new FileInputStream ( fh.getCachedFile () ) );
        JSONTokener tokenizer = new JSONTokener ( reader );
        JSONObject json = new JSONObject ( tokenizer );
        JSONArray subjects = json.getJSONObject ( "ResultSet" ).getJSONArray ( "Result" );
        logger.debug ( "Found: " + subjects.length () + " elements" );
        for ( int idx = 0; idx < subjects.length (); idx++ ) {
          if ( subjects.isNull ( idx ) ) {
            continue;
          }
          String key = subjects.getJSONObject ( idx ).getString ( keyField );
          ArrayList<String> values = new ArrayList<String> ();
          for ( String value : valueFields ) {
            values.add ( subjects.getJSONObject ( idx ).getString ( value ) );
          }
          map.put ( key, values );
        }
      } catch ( Exception e ) {
        logger.error ( "Caught exception reading " + mPath + " from cached file " + fh.getCachedFile ().getAbsolutePath (), e );
        throw new FuseException ();
      } finally {
        if ( fh != null ) {
          fh.release ();
        }
      }
      element = new Element ( mPath + "-HashMap", map );
      xnatfs.sContentCache.put ( element );
    }
    map = (HashMap<String, ArrayList<String>>) element.getObjectValue ();
    return map;
  }

}
