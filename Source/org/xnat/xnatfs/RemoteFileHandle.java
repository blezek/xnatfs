package org.xnat.xnatfs;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.sf.ehcache.Element;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.http.client.*;

import fuse.Errno;

/**
 * New design will download in a background thread, and will handle the read command directly. The downloading thread will append to the end of the random access file, while the read command will seek to the correct offset.
 * 
 * @author blezek
 * 
 */
public class RemoteFileHandle implements Callable<Boolean> {
  private static final Logger logger = Logger.getLogger ( RemoteFileHandle.class );

  public long mLength;
  GetMethod mGet;
  InputStream mStream;
  String mPath;
  String mURL;
  RandomAccessFile mRAF;
  File mFile;
  volatile boolean mDownloadComplete;
  Future<Boolean> mDownloadFuture;
  File mCachedFile;

  public RemoteFileHandle ( String url, String path ) throws Exception {
    mURL = url;
    mPath = path;
    mGet = null;
    mLength = -1;
    mRAF = null;
    mDownloadComplete = false;
    mDownloadFuture = null;
    mCachedFile = null;
    init ();
  }

  public File getCachedFile () {
    return mCachedFile;
  }

  public void waitForDownload () throws Exception {
    if ( mDownloadComplete ) {
      return;
    }
    mDownloadFuture.get ();
  }

  public int read ( ByteBuffer buf, long offset ) throws Exception {
    mRAF = new RandomAccessFile ( mCachedFile, "r" );
    logger.debug ( "Called read " + mPath );
    try {
      if ( offset >= mRAF.length () ) {
        if ( mDownloadComplete ) {
          return Errno.EOVERFLOW;
        }
        // Ok, but we don't have anything ready right now
        logger.debug ( "No data ready" );
        return 0;
      } else {
        // Read what we can
        byte[] buffer = new byte[buf.remaining ()];
        int count = mRAF.read ( buffer );
        buf.put ( buffer, 0, count );
        logger.debug ( "read " + count + " bytes for " + mPath );
        return 0;
      }
    } finally {
      mRAF.close ();
      mRAF = null;
    }
  }

  public void init () throws Exception {
    logger.debug ( "Caching file " + mPath );
    // see if we have contents in the cache
    Element n = xnatfs.sContentCache.get ( mPath );
    if ( n != null ) {
      mCachedFile = (File) n.getObjectValue ();
      if ( mCachedFile.exists () && mCachedFile.canRead () ) {
        mDownloadComplete = true;
        return;
      }
      // File is bogus
      xnatfs.sContentCache.remove ( mPath );
    }
    // Cache it
    mCachedFile = File.createTempFile ( "xnatfsCache", ".tmp" );
    mRAF = new RandomAccessFile ( mCachedFile, "rw" );
    mDownloadFuture = xnatfs.sExecutor.submit ( this );
  }

  public Boolean call () throws Exception {
    mGet = new GetMethod ( mURL );
    HttpClient client = XNATConnection.getInstance ().getClient ();
    HttpGet httpget = new HttpGet ( mURL );
    BasicHttpContext context = new BasicHttpContext ();

    // Generate BASIC scheme object and stick it to the local
    // execution context
    BasicScheme basicAuth = new BasicScheme ();
    context.setAttribute ( "preemptive-auth", basicAuth );
    HttpResponse response = client.execute ( httpget, context );
    logger.debug ( "fetching " + mURL );
    FileOutputStream out = new FileOutputStream ( mCachedFile );
    // Try all at once
    try {
      int TotalCount = 0;
      int NextReportCount = 1000000;
      HttpEntity entity = response.getEntity ();
      byte[] buffer = new byte[4096];
      InputStream in = entity.getContent ();
      logger.debug ( "Fetching remote object " + mURL + " as virtual file " + mPath + " into real file " + mCachedFile );
      while ( true ) {
        int readCount = in.read ( buffer );
        if ( readCount == -1 || entity.isStreaming () ) {
          break;
        }
        out.write ( buffer, 0, readCount );

        // synchronized ( mRAF ) {
        // mRAF.seek ( mRAF.length () );
        // mRAF.write ( buffer, 0, readCount );
        // }
        TotalCount += readCount;
        if ( TotalCount > NextReportCount ) {
          NextReportCount += 1000000;
          logger.debug ( "Read " + readCount + " ( " + TotalCount + " ) from remote file at " + mPath );
        }
      }
      logger.debug ( "Finished fetching " + mURL + " as virtual file " + mPath + " into cache file " + mCachedFile );
      mDownloadComplete = true;
      entity.consumeContent ();
    } catch ( Exception ex ) {
      logger.error ( "Failed to get body of " + mPath + " from URL " + mURL, ex );
      return Boolean.FALSE;
    } catch ( Throwable t ) {
      logger.error ( "Caught throwable for " + mPath + " URL " + mURL, t );
      return Boolean.FALSE;
    } finally {
      out.close ();
    }
    Element n = new Element ( mPath, mCachedFile );
    xnatfs.sContentCache.put ( n );
    logger.debug ( "Cached " + mPath );
    return Boolean.TRUE;
  }

  public void release () {
    if ( mRAF != null ) {
      try {
        mRAF.close ();
      } catch ( IOException e ) {
        logger.error ( "Error closing RandomAccessFile", e );
      }
    }
  }

}
