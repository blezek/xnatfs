package org.xnat.xnatfs;

import java.io.InputStream;

import net.sf.ehcache.Element;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class RemoteFileHandle {
  private static final Logger logger = Logger.getLogger ( RemoteFileHandle.class );

  public long mLength;
  GetMethod mGet;
  InputStream mStream;
  String mPath;
  String mURL;

  public RemoteFileHandle ( String url, String path ) {
    mURL = url;
    mPath = path;
    mGet = null;
    mLength = -1;
  }

  byte[] cache () throws Exception {
    byte[] contents;
    synchronized ( this ) {
      logger.debug ( "Caching file " + mPath );
      // see if we have contents in the cache
      Element n = xnatfs.sContentCache.get ( mPath );
      if ( n != null ) {
        contents = (byte[]) n.getObjectValue ();
      } else {
        // Cache it
        mGet = new GetMethod ( mURL );
        XNATConnection.getInstance ().getClient ().executeMethod ( mGet );
        logger.debug ( "fetching " + mURL );
        // Try all at once
        try {
          contents = mGet.getResponseBody ();
        } catch ( Exception ex ) {
          logger.error ( "Failed to get body of " + mPath + " from URL " + mURL );
          return null;
        }
        logger.debug ( "Got the response" );
        n = new Element ( mPath, contents );
        xnatfs.sContentCache.put ( n );
        logger.debug ( "Cached " + mPath );
        // release ();
        logger.debug ( "Released GET method for " + mPath );
        // byte[] buffer = new byte[4096]; ByteArrayOutputStream out = new ByteArrayOutputStream ( 4096 ); InputStream in = mGet.getResponseBodyAsStream (); while ( true ) { int readCount = in.read ( buffer );
        // logger.debug ( "Read " + readCount + " from remote file at " + mPath ); if ( readCount == -1 ) {
        // Reached the end of the file contents contents = out.toByteArray (); n = new Element ( mPath, contents ); xnatfs.sContentCache.put ( n ); logger.debug ( "Reached the end of the remote file, final size: " + contents.length ); break; } out.write ( buffer, 0, readCount ); } }
      }
    }
    mLength = contents.length;
    return contents;
  }

  public void release () {
    if ( mGet != null ) {
      mGet.releaseConnection ();
      mGet = null;
    }
  }

  public byte[] getBytes () throws Exception {
    byte[] b;
    Element n = xnatfs.sContentCache.get ( mPath );
    if ( n != null ) {
      return (byte[]) n.getObjectValue ();
    } else {
      return cache ();
    }
  }
}
