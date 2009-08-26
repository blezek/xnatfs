/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Resource;

/**
 * @author blezek
 * 
 */
public class Root extends VirtualDirectory {
  /**
   * @param path
   */
  public Root ( XNATFS f, String path, String name ) {
    super ( f, path, name );
  }

  private static final Logger logger = Logger.getLogger ( Root.class );

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
   */
  public Resource child ( String childName ) {
    logger.debug ( "child: " + childName );
    if ( childName.equals ( "projects" ) ) {
      logger.debug ( "Creating: " + childName + " in: " + mAbsolutePath );
      Element element = new Element ( mAbsolutePath + childName, new Projects ( xnatfs, mAbsolutePath, childName ) );
      XNATFS.sNodeCache.put ( element );
      return (Resource) element.getObjectValue ();
    }
    if ( childName.equals ( "hello.txt" ) ) {
      return new DummyFile ( xnatfs, mAbsolutePath, "hello.txt" );
    }
    logger.error ( "Unknown child: " + childName );
    return new DummyFile ( xnatfs, mAbsolutePath, childName );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.CollectionResource#getChildren()
   */
  public List<? extends Resource> getChildren () {
    ArrayList<Resource> list = new ArrayList<Resource> ();
    list.add ( child ( "hello.txt" ) );
    list.add ( child ( "Foo.com" ) );
    list.add ( child ( "projects" ) );
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
