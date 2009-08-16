/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import net.sf.ehcache.Element;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;

/**
 * @author blezek
 * 
 */
public class Root extends Node implements CollectionResource, PropFindableResource {
  /**
   * @param path
   */
  public Root ( xnatfs f, String path ) {
    super ( f, path );
    mName = "/";
  }

  private static final Logger logger = Logger.getLogger ( Root.class );

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
   */
  public Resource child ( String childName ) {
    logger.debug ( "child: " + childName );
    if ( false && childName.equals ( "projects" ) ) {
      logger.debug ( "Creating: " + childName + " in: " + mPath );
      Element element = new Element ( mPath + childName, new Projects ( factory, mPath + childName ) );
      xnatfs.sNodeCache.put ( element );
      return (Resource) element.getObjectValue ();
    }
    if ( childName.equals ( "hello.txt" ) ) {
      return new DummyFile ( factory, "hello.txt" );
    }
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#getChildren()
   */
  public List<? extends Resource> getChildren () {
    ArrayList<Resource> list = new ArrayList<Resource> ();
    list.add ( child ( "hello.txt" ) );
    return list;
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

  public Date getCreateDate () {
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
