/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.List;

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
    // TODO Auto-generated constructor stub
  }

  /* (non-Javadoc)
   * @see org.xnat.xnatfs.webdav.VirtualDirectory#child(java.lang.String)
   */
  @Override
  public Resource child ( String arg0 ) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.xnat.xnatfs.webdav.VirtualDirectory#getChildren()
   */
  @Override
  public List<? extends Resource> getChildren () {
    // TODO Auto-generated method stub
    return null;
  }

}
