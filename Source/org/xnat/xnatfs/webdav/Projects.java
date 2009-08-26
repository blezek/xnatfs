/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Resource;

/**
 * @author blezek
 * 
 */
public class Projects extends VirtualDirectory implements CollectionResource {
  private static final Logger logger = Logger.getLogger ( Projects.class );

  /**
   * @param string
   */
  public Projects ( XNATFS f, String path, String name ) {
    super ( f, path, name );
    mChildKey = "id";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
   */
  public Resource child ( String childName ) {
    logger.debug ( "child: create " + childName );
    if ( childName.equals ( "Image1.dcm" ) ) {
      return new RemoteFile ( xnatfs, mAbsolutePath, childName,
          "/projects/NAMIC_TEST/subjects/CENTRAL_S00266/experiments/CENTRAL_E00469/scans/4/resources/123150667/files/1.MR.head_DHead.4.176.20061214.091206.156000.9694718604.dcm", 191908L );
    }
    if ( childName.equals ( "Image2.dcm" ) ) {
      return new RemoteFile ( xnatfs, mAbsolutePath, childName,
          "/projects/NAMIC_TEST/subjects/CENTRAL_S00266/experiments/CENTRAL_E00469/scans/4/resources/123150667/files/1.MR.head_DHead.4.175.20061214.091206.156000.1443018602.dcm", 191908L );
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#getChildren()
   */
  public List<? extends Resource> getChildren () {
    ArrayList<Resource> list = new ArrayList<Resource> ();
    list.add ( child ( "Image1.dcm" ) );
    list.add ( child ( "Image2.dcm" ) );
    return list;
  }

}
