/**
 * 
 */
package org.xnat.xnatfs;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import fuse.Errno;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtypeConstants;
import fuse.FuseGetattrSetter;

/**
 * @author blezek
 * 
 */
public class Resource extends Node {
  private static final Logger logger = Logger.getLogger ( Resource.class );

  public Resource ( String path ) {
    super ( path );
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.Node#getattr(java.lang.String, fuse.FuseGetattrSetter)
   */
  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  protected HashSet<String> getFiles () throws FuseException {
    // Get the subjects code
    Element element = xnatfs.sContentCache.get ( mPath );
    HashSet<String> list = null;
    if ( element == null ) {
      RemoteFileHandle fh = null;
      try {
        fh = XNATConnection.getInstance ().get ( mPath + "?format=json", mPath );
        list = new HashSet<String> ();
        fh.waitForDownload ();
        InputStreamReader reader = new InputStreamReader ( new FileInputStream ( fh.getCachedFile () ) );
        SAXBuilder builder = new SAXBuilder ();
        Document doc = builder.build ( reader );
        org.jdom.Element root = doc.getRootElement ();

        String id = "foo";
        list.add ( id );

      } catch ( Exception e ) {
        logger.error ( "Caught exception reading " + mPath, e );
        throw new FuseException ();
      } finally {
        if ( fh != null ) {
          fh.release ();
        }
      }
      element = new Element ( mPath, list );
      xnatfs.sContentCache.put ( element );
    }
    list = (HashSet<String>) element.getObjectValue ();
    return list;
  }

  @Override
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      HashSet<String> resources = getFiles ();
      for ( String resource : resources ) {
        createChild ( resource );
        filler.add ( resource, resource.hashCode (), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

}
