/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;

/**
 * @author blezek
 * 
 */
public class Projects extends Container implements CollectionResource {
  private static final Logger logger = Logger.getLogger ( Projects.class );

  /**
   * @param string
   */
  public Projects ( xnatfs f, String path ) {
    super ( f, path );
    mChildKey = "id";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
   */
  public Resource child ( String childName ) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#getChildren()
   */
  public List<? extends Resource> getChildren () {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authenticate(java.lang.String,
   * java.lang.String)
   */
  public Object authenticate ( String user, String password ) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authorise(com.bradmcevoy.http.Request,
   * com.bradmcevoy.http.Request.Method, com.bradmcevoy.http.Auth)
   */
  public boolean authorise ( Request request, Method method, Auth auth ) {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.bradmcevoy.http.Resource#checkRedirect(com.bradmcevoy.http.Request)
   */
  public String checkRedirect ( Request request ) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getModifiedDate()
   */
  public Date getModifiedDate () {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getName()
   */
  public String getName () {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getRealm()
   */
  public String getRealm () {
    // TODO Auto-generated method stub
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
