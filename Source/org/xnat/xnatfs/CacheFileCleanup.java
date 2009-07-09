/**
 * 
 */
package org.xnat.xnatfs;

import java.io.File;

import org.apache.log4j.Logger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

/**
 * @author blezek
 * 
 */
public class CacheFileCleanup implements CacheEventListener {
  private static final Logger logger = Logger.getLogger ( CacheFileCleanup.class );

  /*
   * (non-Javadoc)
   * 
   * @see
   * net.sf.ehcache.event.CacheEventListener#notifyElementEvicted(net.sf.ehcache
   * .Ehcache, net.sf.ehcache.Element)
   */
  public void notifyElementEvicted ( Ehcache arg0, Element element ) {
    // TODO Auto-generated method stub
    Object contents = element.getObjectValue ();
    if ( contents instanceof File ) {
      // Delete the file
      File file = (File) contents;
      logger.debug ( "Element evicted ( " + element.getObjectKey () + " ), deleting: " + file.getAbsolutePath () );
      file.delete ();
    }
  }

  // Do nothing for most of these
  public void notifyElementExpired ( Ehcache arg0, Element arg1 ) {
    // TODO Auto-generated method stub
  }

  public void dispose () {
    // TODO Auto-generated method stub
  }

  public void notifyElementPut ( Ehcache arg0, Element arg1 ) throws CacheException {
    // TODO Auto-generated method stub
  }

  public void notifyElementRemoved ( Ehcache arg0, Element arg1 ) throws CacheException {
    // TODO Auto-generated method stub
  }

  public void notifyElementUpdated ( Ehcache arg0, Element arg1 ) throws CacheException {
    // TODO Auto-generated method stub
  }

  public void notifyRemoveAll ( Ehcache cache ) {
    logger.debug ( "All removed, but " + cache.getSize () + " are still in the cache" );
  }

  @Override
  public Object clone () throws CloneNotSupportedException {
    // TODO Auto-generated method stub
    return super.clone ();
  }

}
