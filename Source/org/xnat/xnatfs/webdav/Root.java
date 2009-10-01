/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.HashSet;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Resource;

/**
 * @author blezek
 * 
 */
public class Root extends VirtualDirectory {
  /**
   * @param path
   */
  public Root ( XNATFS f, String path, String name, String url ) {
    super ( f, path, name, url );
    mElementURL = mURL + "projects?format=json";
  }

  private static final Logger logger = Logger.getLogger ( Root.class );

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
   */
  public Resource child ( String childName ) {
    HashSet<String> s = null;
    try {
      s = getElementList ( mURL + "projects?format=json", mChildKey );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( true || s.contains ( childName ) ) {
      logger.debug ( "child: Creating child " + childName );
      Project project = new Project ( xnatfs, mAbsolutePath, childName, mURL + "projects/" + childName + "/" );
      setChildAuthorization ( project );
      return project;
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#getChildren()
   */
  /*
   * public List<? extends Resource> getChildren () { HashSet<String> s = null;
   * try { s = getElementList ( mAbsolutePath + "projects?format=json",
   * mChildKey ); } catch ( Exception e ) { logger.error (
   * "Failed to get child element list: " + e ); } ArrayList<Resource> list =
   * new ArrayList<Resource> (); for ( String child : s ) { // logger.debug (
   * "got Child " + child ); list.add ( child ( child ) ); } return list; }
   */

}
