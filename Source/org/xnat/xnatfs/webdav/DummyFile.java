/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 * @author blezek
 * 
 */
public class DummyFile implements PropFindableResource, GetableResource {
  private static final Logger logger = Logger.getLogger ( DummyFile.class );

  final xnatfs factory;
  String mName;

  public DummyFile ( xnatfs f, String name ) {
    factory = f;
    mName = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authenticate(java.lang.String,
   * java.lang.String)
   */
  public Object authenticate ( String user, String password ) {
    return factory.getSecurityManager ().authenticate ( user, password );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authorise(com.bradmcevoy.http.Request,
   * com.bradmcevoy.http.Request.Method, com.bradmcevoy.http.Auth)
   */
  public boolean authorise ( Request request, Method method, Auth auth ) {
    return factory.getSecurityManager ().authorise ( request, method, auth, this );
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
    return mName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getRealm()
   */
  public String getRealm () {
    return factory.getRealm ();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getUniqueId()
   */
  public String getUniqueId () {
    // TODO Auto-generated method stub
    return mName;
  }

  static final String sContents = "Hello World!\n";

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.GetableResource#getContentLength()
   */
  public Long getContentLength () {
    return new Long ( sContents.length () );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.GetableResource#getContentType(java.lang.String)
   */
  public String getContentType ( String accepts ) {
    logger.debug ( "getContentType: " + accepts );
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.bradmcevoy.http.GetableResource#getMaxAgeSeconds(com.bradmcevoy.http
   * .Auth)
   */
  public Long getMaxAgeSeconds ( Auth auth ) {
    return new Long ( 1200 );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.GetableResource#sendContent(java.io.OutputStream,
   * com.bradmcevoy.http.Range, java.util.Map, java.lang.String)
   */
  public void sendContent ( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException {
    if ( range != null ) {
      // Just want a portion of the file
      logger.debug ( "sendContent start: " + range.getStart () + " finish: " + range.getFinish () + " response: " + sContents.substring ( (int) range.getStart (), (int) range.getFinish () ) );
      out.write ( sContents.substring ( (int) range.getStart (), (int) range.getFinish () ).getBytes () );
    } else {
      // Write it all out
      out.write ( sContents.getBytes () );
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.PropFindableResource#getCreateDate()
   */
  public Date getCreateDate () {
    // TODO Auto-generated method stub
    return new Date ();
  }
}
