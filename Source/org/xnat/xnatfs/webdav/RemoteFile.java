package org.xnat.xnatfs.webdav;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.sf.ehcache.Element;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.log4j.Logger;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import fuse.Errno;

public class RemoteFile extends VirtualFile {
  String mURL;
  Long mContentLength;
  FileChannel mChannel = null;
  RandomAccessFile mFile = null;
  boolean mDownloadComplete = false;
  private Future<Boolean> mDownloadFuture;
  File mCachedFile = null;
  private static final Logger logger = Logger.getLogger ( RemoteFile.class );

  public RemoteFile ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name );
    if ( url == null ) {
      mURL = path + name;
    } else {
      mURL = url;
    }
  }

  public void setContentLength ( Long l ) {
    mContentLength = l;
  }

  public Long getContentLength () {
    return mContentLength;
  }

  public void sendContent ( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException {
    open ();
    if ( mFile.getFD ().valid () == false ) {
      logger.error ( "File description for " + mPath + " is not valid" );
    }

    long start = 0, end = mContentLength;
    if ( range != null ) {
      start = range.getStart ();
      end = range.getFinish ();
    }
    logger.debug ( "Current size: " + mChannel.size () );
    long currentSize = mChannel.size ();
    long sleepTime = 0;
    long millis = 200;
    while ( end < mChannel.size () ) {
      // Wait
      if ( mDownloadComplete ) {
        throw new IOException ( "Underflow" );
      }

      // Ok, but we don't have anything ready right now, sleep for up to 10
      // second
      Thread.sleep ( millis );
      sleepTime += millis;
      if ( currentSize != mChannel.size() ) {
        currentSize = mChannel.size ();
        sleepTime = 0;
      }
        if ( sleepTime > 30000 ) {
          logger.error ( "Failed to wait for " + mPath + " waited " + sleepTime / 1000. + "seconds" );
          throw new IOException ( "No download progress for 30 seconds");
        }
      }
    }
  // Read it out
    // Read what we can
    int count = mChannel.read ( buf, offset );
    logger.debug ( "read " + count + " bytes for " + mPath );
    return 0;
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

  void open () throws Exception {
    // see if we have contents in the cache
    logger.debug ( "Opening " + mPath + " from remote URL " + mURL );
    Element n = XNATFS.sContentCache.get ( mPath );
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
        XNATFS.sContentCache.remove ( mPath );
        mCachedFile = File.createTempFile ( "xnatfsCache", ".tmp", XNATFS.sTemporaryDirectory );
      }
    } else {
      // Cache it
      mCachedFile = File.createTempFile ( "xnatfsCache", ".tmp", XNATFS.sTemporaryDirectory );
    }
    logger.debug ( "Caching remote file to " + mCachedFile );
    mFile = new RandomAccessFile ( mCachedFile, "rw" );
    mChannel = mFile.getChannel ();
    if ( !mDownloadComplete ) {
      logger.debug ( "Starting background download" );
      mDownloadFuture = XNATFS.sExecutor.submit ( new BackgroundFetch () );
    }
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
          if ( Thread.currentThread ().isInterrupted () ) {
            logger.debug ( "Thread was interupted, finishing download" );
            break;
          }
          if ( readCount == -1 || entity.isStreaming () ) {
            logger.debug ( "Finished reading " + mURL );
            break;
          }
          buffer.limit ( readCount );
          buffer.position ( 0 );
          synchronized ( mChannel ) {
            logger.debug ( "Writing " + readCount + " bytes to virtual file " + mPath + " for URL: " + mURL );
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
      XNATFS.sContentCache.put ( n );
      logger.debug ( "Cached " + mPath );
      return Boolean.TRUE;
    }
  }

}
