package org.xnat.xnatfs.webdav;

import java.util.Date;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;

public abstract class VirtualResource implements Resource, PropFindableResource {
  public static final Date mDate = new Date ();
  String mPath;
  String mName;
  XNATFS xnatfs;
  String mAbsolutePath;

  public VirtualResource ( XNATFS x, String path, String name ) {
    xnatfs = x;
    mPath = path;
    mName = name;
    if ( path == null && mName.equals ( "/" ) ) {
      mAbsolutePath = "/";
    } else {
      mAbsolutePath = path + name;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authenticate(java.lang.String,
   * java.lang.String)
   */
  public Object authenticate ( String user, String password ) {
    return xnatfs.getSecurityManager ().authenticate ( user, password );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authorise(com.bradmcevoy.http.Request,
   * com.bradmcevoy.http.Request.Method, com.bradmcevoy.http.Auth)
   */
  public boolean authorise ( Request request, Method method, Auth auth ) {
    return xnatfs.getSecurityManager ().authorise ( request, method, auth, this );
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
