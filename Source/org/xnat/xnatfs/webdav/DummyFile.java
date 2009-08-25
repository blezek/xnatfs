/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 * @author blezek
 * 
 */
public class DummyFile extends VirtualFile implements GetableResource {
  private static final Logger logger = Logger.getLogger ( DummyFile.class );

  public DummyFile ( XNATFS f, String path, String name ) {
    super ( f, path, name );
  }

  static final String sContents = "Hello World!\n";

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.GetableResource#getContentLength()
   */
  public Long getContentLength () {
    return new Long ( 1024 * 1024 );
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
    byte[] c = new byte[1];
    c[0] = 0;
    if ( range != null ) {
      // Just want a portion of the file
      logger.debug ( "sendContent start: " + range.getStart () + " finish: " + range.getFinish () + " response: " + sContents.substring ( (int) range.getStart (), (int) range.getFinish () ) );
      for ( long start = range.getStart (); start < range.getFinish (); start++ ) {
        out.write ( c );
      }
      // out.write ( sContents.substring ( (int) range.getStart (), (int)
      // range.getFinish () ).getBytes () );
    } else {
      logger.debug ( "setContent request for entire file" );
      // Write it all out
      for ( long i = 0; i < 1024 * 1024; i++ ) {
        out.write ( c );
      }
      logger.debug ( "Finished sending entire file" );
      // out.write ( sContents.getBytes () );
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
