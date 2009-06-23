package org.xnat.xnatfs;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
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

/**
 * A utility class for the pattern in the XNAT REST api for an element that
 * contains a list of sub-element. For instance, the <code>/projects/</code> api
 * returns a list of contained projects. By default, the Container class fetches
 * from the REST url in the JSON format. By parsing the JSON, Container finds
 * child elements using the <code>mChildKey</code> as an index into the JSON.
 * For each child found, Container calls <code>createChild</code> and adds the
 * child in the <code>getDir</code>.
 * 
 * @author Daniel Blezek
 * 
 */
public abstract class Container extends Node {
  private static final Logger logger = Logger.getLogger ( Container.class );
  String mChildKey = "id";

  public Container ( String path ) {
    super ( path );
  }

  /**
   * Get sub-elements of this element. Return a set of names. Used to populate
   * the directory, and calls <code>createChild</code>, delegating to
   * subclasses.
   * 
   * @return Set of strings of the contained elements.
   * @throws FuseException
   */
  protected HashSet<String> getElementList () throws FuseException {
    // Get the subjects code
    Element element = xnatfs.sContentCache.get ( mPath );
    HashSet<String> list = null;
    if ( element == null ) {
      RemoteFileHandle fh = null;
      try {
        fh = XNATConnection.getInstance ().get ( mPath + "?format=json", mPath );
        list = new HashSet<String> ();
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
          String id = subjects.getJSONObject ( idx ).getString ( mChildKey );
          list.add ( id );
        }
      } catch ( Exception e ) {
        logger.error ( "Caught exception reading " + mPath, e );
        throw new FuseException ();
      } finally {
        if ( fh != null ) {
          fh.release ();
        }
      }
      element = new Element ( mPath, list );
      xnatfs.sContentCache.put ( element );
    }
    list = (HashSet<String>) element.getObjectValue ();
    return list;
  }

  /*
   * Fill in the directory specified by this path. Uses the <code>filler</code>
   * to add children obtained from <code>getElementList</code>. Also calls
   * <code>createChild</code> to create a Node. The subclass decides the type of
   * its children.
   * 
   * @see org.xnat.xnatfs.Node#getdir(java.lang.String, fuse.FuseDirFiller)
   */
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      HashSet<String> projectList = getElementList ();
      for ( String project : projectList ) {
        createChild ( project );
        filler.add ( project, project.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      String t = tail ( mPath );
      for ( String extention : RemoteFile.sExtensions ) {
        String c = t + extention;
        filler.add ( c, c.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  /*
   * Default implementation. Creates a RemoteListFile corresponding to the
   * element from the REST api. This is generally a <code>path.xml</code>,
   * <code>path.json</code>, <code>path.html</code> and/or
   * <code>path.csv</code>.
   * 
   * @see org.xnat.xnatfs.Node#createChild(java.lang.String)
   */
  public Node createChild ( String child ) throws FuseException {
    logger.debug ( "createChild: " + child + " in path " + mPath );
    if ( child.startsWith ( tail ( mPath ) ) && RemoteFile.sExtensions.contains ( extention ( child ) ) ) {
      logger.debug ( "Create child " + child + " of " + mPath );
      String childPath = mPath + "/" + child;
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new RemoteFile ( childPath, extention ( child ), mPath + extention ( child ) ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return null;
  }

}
