/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.HashSet;

import net.sf.ehcache.Element;

import com.bradmcevoy.http.Resource;

/**
 * @author blezek
 * 
 */
public class Experiment extends VirtualDirectory {

  /**
   * @param x
   * @param path
   * @param name
   * @param url
   */
  public Experiment ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name, url );
    mElementURL = mURL + "scans?format=json";
    mChildKey = "id";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.webdav.VirtualDirectory#child(java.lang.String)
   */
  @Override
  public Resource child ( String childName ) {
    logger.debug ( "child: create " + childName );
    String childPath = mAbsolutePath + childName;
    HashSet<String> s = null;
    try {
      s = getElementList ( mElementURL, mChildKey );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( s.contains ( childName ) ) {
      // Look up in the cache
      if ( XNATFS.sNodeCache.get ( childPath ) != null ) {
        return (Resource) ( XNATFS.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Scan ( xnatfs, mAbsolutePath, childName, mURL + "scans/" + childName + "/" ) );
      XNATFS.sNodeCache.put ( element );
      return (Resource) element.getObjectValue ();
    }
    return null;
  }

}