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
    return new DummyFile ( xnatfs, mAbsolutePath, childName );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#getChildren()
   */
  public List<? extends Resource> getChildren () {
    ArrayList<Resource> list = new ArrayList<Resource> ();
    list.add ( child ( "Foo.txt" ) );
    list.add ( child ( "bar.txt" ) );
    return list;
  }

}
