/**
 * 
 */
package org.xnat.xnatfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.log4j.Logger;

import fuse.Errno;

import net.sf.ehcache.Element;

/**
 * @author blezek
 * 
 */
public class FileHandle {

  FileChannel mChannel = null;
  RandomAccessFile mFile = null;
  String mURL = null;
  String mPath = null;
  boolean mDownloadComplete = false;
  private Future<Boolean> mDownloadFuture;
  File mCachedFile = null;
  private static final Logger logger = Logger.getLogger ( FileHandle.class );

  public FileHandle ( String url, String path ) {
    mURL = url;
    mPath = path;
    mDownloadComplete = false;
  }

  public boolean isDownloadComplete () {
    return mDownloadComplete;
  }

  public long waitForDownload () throws Exception {
    if ( mDownloadFuture != null ) {
      logger.debug ( "Waiting on " + mDownloadFuture + " isCanceled " + mDownloadFuture.isCancelled () + " isDone " + mDownloadFuture.isDone () );
      mDownloadFuture.get ();
    }
    return mChannel.size ();
  }

  public void open () throws Exception {
    // see if we have contents in the cache
    logger.debug ( "Opening " + mPath + " from remote URL " + mURL );
    Element n = xnatfs.sContentCache.get ( mPath );
    mCachedFile = null;
    if ( n != null ) {
      mCachedFile = (File) n.getObjectValue ();
      logger.debug ( "Found cached file for " + mPath + " at " + mCachedFile );
      if ( mCachedFile.exists () && mCachedFile.canRead () ) {
        logger.debug ( "File exists and is readable" );
        mDownloadComplete = true;
      } else {
        // File is bogus
        logger.debug ( "Cached file was not readable or didn't exist" );
        xnatfs.sContentCache.remove ( mPath );
        mCachedFile = File.createTempFile ( "xnatfsCache", ".tmp", xnatfs.sTemporaryDirectory );
      }
    } else {
      // Cache it
      mCachedFile = File.createTempFile ( "xnatfsCache", ".tmp", xnatfs.sTemporaryDirectory );
    }
    logger.debug ( "Caching remote file to " + mCachedFile );
    mFile = new RandomAccessFile ( mCachedFile, "rw" );
    mChannel = mFile.getChannel ();
    if ( !mDownloadComplete ) {
      logger.debug ( "Starting background download" );
      mDownloadFuture = xnatfs.sExecutor.submit ( new BackgroundFetch () );
    }
  }

  public int read ( ByteBuffer buf, long offset ) throws Exception {
    logger.debug ( "Called read " + mPath );
    if ( mFile.getFD ().valid () == false ) {
      logger.error ( "File description for " + mPath + " is not valid" );
    }
    logger.debug ( "Current size: " + mChannel.size () );
    synchronized ( mChannel ) {
      if ( offset >= mChannel.size () ) {
        if ( mDownloadComplete ) {
          return Errno.EOVERFLOW;
        }
        // Ok, but we don't have anything ready right now
        logger.debug ( "No data ready" );
        return 0;
      } else {
        // Read what we can
        int count = mChannel.read ( buf, offset );
        logger.debug ( "read " + count + " bytes for " + mPath );
        return 0;
      }
    }
  }

  public void release () throws Exception {
    // TODO Auto-generated method stub
    if ( mDownloadFuture != null && !mDownloadFuture.isDone () ) {
      mDownloadFuture.cancel ( true );
    }
    mChannel.close ();
    mFile.close ();
  }

  class BackgroundFetch implements Callable<Boolean> {
    public Boolean call () throws Exception {
      // TODO Auto-generated method stub
      logger.debug ( "Starting remote background fetch for " + mURL + " as file " + mPath );
      // GetMethod get = new GetMethod ( mURL );
      try {
        HttpClient client = XNATConnection.getInstance ().getClient ();
        HttpGet httpget = new HttpGet ( mURL );
        BasicHttpContext context = new BasicHttpContext ();

        // Generate BASIC scheme object and stick it to the local
        // execution context
        BasicScheme basicAuth = new BasicScheme ();
        context.setAttribute ( "preemptive-auth", basicAuth );
        HttpResponse response = client.execute ( httpget, context );
        logger.debug ( "fetching " + mURL );
        // Try all at once

        int TotalCount = 0;
        int NextReportCount = 1000000;
        HttpEntity entity = response.getEntity ();
        byte[] BackingBuffer = new byte[4096];
        ByteBuffer buffer = ByteBuffer.wrap ( BackingBuffer );
        InputStream in = entity.getContent ();
        logger.debug ( "Fetching remote object " + mURL + " as virtual file " + mPath + " into real file " );
        while ( true ) {
          int readCount = in.read ( BackingBuffer );
          if ( readCount == -1 || entity.isStreaming () ) {
            break;
          }
          buffer.limit ( readCount );
          buffer.position ( 0 );
          synchronized ( mChannel ) {
            mChannel.write ( buffer );
          }
          TotalCount += readCount;
          if ( TotalCount > NextReportCount ) {
            NextReportCount += 1000000;
            logger.debug ( "Read " + readCount + " ( " + TotalCount + " ) from remote file at " + mPath );
          }
        }
        logger.debug ( "Finished fetching " + mURL + " as virtual file " + mPath + " into cache file" );
        mDownloadComplete = true;
        entity.consumeContent ();
      } catch ( Exception ex ) {
        logger.error ( "Failed to get body of " + mPath + " from URL " + mURL, ex );
        return Boolean.FALSE;
      } catch ( Throwable t ) {
        logger.error ( "Caught throwable for " + mPath + " URL " + mURL, t );
        return Boolean.FALSE;
      }
      Element n = new Element ( mPath, mCachedFile );
      xnatfs.sContentCache.put ( n );
      logger.debug ( "Cached " + mPath );
      return Boolean.TRUE;
    }
  }

}
