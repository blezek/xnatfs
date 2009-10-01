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
    mFallbackChildKey = "xnat_abstractresource_id";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.webdav.VirtualDirectory#child(java.lang.String)
   */
  @Override
  public Resource child ( String childName ) {
    logger.debug ( "child: create " + childName );
    HashSet<String> s = null;
    try {
      s = getElementList ( mElementURL, null );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( true || s.contains ( childName ) ) {
      Bundle bundle = new Bundle ( xnatfs, mAbsolutePath, childName, mURL + "resources/" + childName + "/" );
      setChildAuthorization ( bundle );
      return bundle;
    }
    return null;
  }
}
