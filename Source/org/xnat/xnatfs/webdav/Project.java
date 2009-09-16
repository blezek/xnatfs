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
public class Project extends VirtualDirectory {
  final static Logger logger = Logger.getLogger ( Project.class );

  /**
   * @param x
   * @param path
   * @param name
   */
  public Project ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name, url );
    mChildKey = "label";
    mElementURL = mURL + "subjects?format=json";
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
      s = getElementList ( mElementURL, mChildKey );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( s.contains ( childName ) ) {
      // Look up in the cache
      return new Subject ( xnatfs, mAbsolutePath, childName, mURL + "subjects/" + childName + "/" );
    }
    if ( childName.equals ( "Image1.dcm" ) ) {
      return new RemoteFile ( xnatfs, mAbsolutePath, childName,
          "/projects/NAMIC_TEST/subjects/1/experiments/MR1/scans/4/resources/DICOM/files/1.MR.head_DHead.4.176.20061214.091206.156000.9694718604.dcm", 191908L );
    }
    if ( childName.equals ( "Image2.dcm" ) ) {
      return new RemoteFile ( xnatfs, mAbsolutePath, childName,
          "/projects/NAMIC_TEST/subjects/1/experiments/MR1/scans/4/resources/DICOM/files/1.MR.head_DHead.4.167.20061214.091206.156000.1886718586.dcm", 191902L );
    }
    return null;
  }

}
