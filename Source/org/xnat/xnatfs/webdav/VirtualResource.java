package org.xnat.xnatfs.webdav;

import java.util.Date;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;

public abstract class VirtualResource implements Resource, PropFindableResource {
  final static Logger logger = Logger.getLogger ( VirtualResource.class );

  public static final Date mDate = new Date ();
  String mPath;
  String mName;
  XNATFS xnatfs;
  String mAbsolutePath;
  Auth mCredentials;

  public VirtualResource ( XNATFS x, String path, String name ) {
    xnatfs = x;
    mPath = path;
    mName = name;
    mCredentials = null;
    if ( path == null && mName.equals ( "/" ) ) {
      mAbsolutePath = "/";
    } else {
      if ( path.equals ( "/" ) ) {
        mAbsolutePath = path + mName;
      } else {
        mAbsolutePath = path + "/" + mName;
      }
    }
    // mCredentials = new Auth ( new String ( Base64.encodeBase64 ( new String (
    // "blezek:throwaway" ).getBytes () ) ) );
    // logger.debug ( "Created virtual resource with name " + mName + " path " +
    // mPath + " absolute Path " + mAbsolutePath );
  }

  public Auth getCredentials () {
    return mCredentials;
  }

  public void setCredentials ( Auth c ) {
    mCredentials = c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authenticate(java.lang.String,
   * java.lang.String)
   */
  public Object authenticate ( String user, String password ) {
    logger.debug ( "authenticate: Calling from class " + this.getClass ().getName () );
    return xnatfs.getSecurityManager ().authenticate ( user, password );
  }

  void setChildAuthorization ( VirtualResource child ) {
    child.authorise ( null, null, mCredentials );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authorise(com.bradmcevoy.http.Request,
   * com.bradmcevoy.http.Request.Method, com.bradmcevoy.http.Auth)
   */
  public boolean authorise ( Request request, Method method, Auth auth ) {
    if ( auth == null ) {
      return false;
    }
    mCredentials = auth;
    logger.debug ( "authorise " + auth.user + " for class " + this.getClass ().getName () );
    // return xnatfs.getSecurityManager ().authorise ( request, method, auth,
    // this );
    return true;
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
    return mDate;
  }

  public Date getCreateDate () {
    // TODO Auto-generated method stub
    return mDate;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getName()
   */
  public String getName () {
    return mName;
  }

  public String getAbsolutePath () {
    return mAbsolutePath;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getRealm()
   */
  public String getRealm () {
    return xnatfs.getRealm ();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#getUniqueId()
   */
  public String getUniqueId () {
    // TODO Auto-generated method stub
    return mAbsolutePath;
  }

  /**
   * Get the root of the path, i.e. everything up to the last "."
   */
  static public String root ( String path ) {
    int idx = path.lastIndexOf ( "." );
    if ( idx <= 0 ) {
      return path;
    }
    return path.substring ( 0, idx );
  }

  /**
   * Get the tail of the path, i.e. everything past the last "/"
   */
  static public String tail ( String path ) {
    if ( path.endsWith ( "/" ) ) {
      return tail ( path.substring ( 0, path.length () - 1 ) );
    }
    int idx = path.lastIndexOf ( "/" );
    if ( idx < 0 ) {
      return path;
    }
    return path.substring ( idx + 1 );
  }

  /**
   * Get the tail of the path, i.e. everything past the last "/"
   */
  static public String dirname ( String path ) {
    if ( path.endsWith ( "/" ) ) {
      return dirname ( path.substring ( 0, path.length () - 1 ) );
    }
    int idx = path.lastIndexOf ( "/" );
    if ( idx < 0 ) {
      return path;
    }
    if ( idx == 0 ) {
      return "/";
    }
    return path.substring ( 0, idx );
  }

  /**
   * Get the extention of the path, i.e. everything past the last "."
   */
  static public String extention ( String path ) {
    int idx = path.lastIndexOf ( "." );
    if ( idx == -1 ) {
      return "";
    }
    return path.substring ( path.lastIndexOf ( "." ) );
  }

}
