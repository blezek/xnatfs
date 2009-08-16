/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.xnat.xnatfs.webdav.*;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;

/**
 * @author blezek
 * 
 */
public abstract class Node implements Serializable, CollectionResource {
  protected String mPath;
  protected String mName;
  final xnatfs factory;

  public Node ( xnatfs f, String path ) {
    factory = f;
    this.mPath = path;
  }

  // public API
  public synchronized String getPath () {
    return mPath;
  }

  public synchronized void setPath ( String name ) {
    this.mPath = name;
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

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
   */
  public abstract Resource child ( String childName );

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#getChildren()
   */
  public abstract List<? extends Resource> getChildren ();

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
    return new Date ();
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
    return mPath + mName;
  }

}
