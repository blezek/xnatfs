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
public class Subject extends VirtualDirectory {
  final static Logger logger = Logger.getLogger ( Subject.class );

  /**
   * @param x
   * @param path
   * @param name
   * @param url
   */
  public Subject ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name, url );
    mElementURL = mURL + "experiments?format=json";
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
    HashSet<String> s = null;
    try {
      s = getElementList ( mElementURL, null );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( true || s.contains ( childName ) ) {
      return new Experiment ( xnatfs, mAbsolutePath, childName, mURL + "experiments/" + childName + "/" );
    }
    return null;
  }
}
