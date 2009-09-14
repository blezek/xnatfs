/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.Date;
import java.util.HashSet;

import net.sf.ehcache.Element;

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
    String childPath = mAbsolutePath + childName;
    HashSet<String> s = null;
    try {
      s = getElementList ( mURL + "projects?format=json", mChildKey );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( s.contains ( childName ) ) {
      // Look up in the cache
      if ( XNATFS.sNodeCache.get ( childPath ) != null ) {
        return (Resource) ( XNATFS.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Project ( xnatfs, mAbsolutePath, childName, mURL + "projects/" + childName + "/" ) );
      XNATFS.sNodeCache.put ( element );
      return (Resource) element.getObjectValue ();
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

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getModifiedDate()
   */
  public Date getModifiedDate () {
    // TODO Auto-generated method stub
    return null;
  }

  public Date getCreateDate () {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getUniqueId()
   */
  public String getUniqueId () {
    // TODO Auto-generated method stub
    return null;
  }

}
