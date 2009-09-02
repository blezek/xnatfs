/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.HashSet;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Resource;

/**
 * @author blezek
 * 
 */
public class Scan extends VirtualDirectory {
  static final Logger logger = Logger.getLogger ( Scan.class );

  /**
   * @param x
   * @param path
   * @param name
   * @param url
   */
  public Scan ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name, url );
    mElementURL = mURL + "resources?format=json";
    mChildKey = "label";
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
      s = getElementList ( mElementURL, null );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( s.contains ( childName ) ) {
      // Look up in the cache
      if ( XNATFS.sNodeCache.get ( childPath ) != null ) {
        return (Resource) ( XNATFS.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Bundle ( xnatfs, mAbsolutePath, childName, mURL + "resources/" + childName + "/" ) );
      XNATFS.sNodeCache.put ( element );
      return (Resource) element.getObjectValue ();
    }
    return null;
  }
}
