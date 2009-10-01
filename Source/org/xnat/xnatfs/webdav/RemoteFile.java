package org.xnat.xnatfs.webdav;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class RemoteFile extends VirtualFile {
  String mURL;
  Long mContentLength;
  FileChannel mChannel = null;
  RandomAccessFile mFile = null;
  boolean mDownloadComplete = false;
  private Future<Boolean> mDownloadFuture;
  File mCachedFile = null;
  private static final Logger logger = Logger.getLogger ( RemoteFile.class );

  public RemoteFile ( XNATFS x, String path, String name, String url, Long length ) {
    super ( x, path, name );
    if ( url == null ) {
      mURL = path + name;
    } else {
      mURL = url;
    }
    mContentLength = length;
    logger.debug ( "RemoteFile constructor absolute path is: " + mAbsolutePath + " length: " + length );
  }

  public void setContentLength ( Long l ) {
    logger.debug ( "setContentLength: " + l );
    mContentLength = l;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.Resource#authorise(com.bradmcevoy.http.Request,
   * com.bradmcevoy.http.Request.Method, com.bradmcevoy.http.Auth)
   */
  public boolean authorise ( Request request, Method method, Auth auth ) {
    boolean b = super.authorise ( request, method, auth );
    logger.debug ( "authorise: checking for existance on the remote server" );
    return b && getContentLength () != -1L;
  }

  public Long getContentLength () {
    logger.debug ( "getContentLength: " + mContentLength );
    if ( mContentLength != -1 ) {
      return mContentLength;
    }
    // Get the size from the parent
    String parent = dirname ( mURL );
    // Remove the trailing / if any
    if ( parent.endsWith ( "/" ) ) {
      parent = parent.substring ( 0, parent.length () - 2 );
    }
    HashMap<String, ArrayList<String>> s = null;
    try {
      ArrayList<String> l = new ArrayList<String> ();
      l.add ( "URI" );
      l.add ( "Size" );
      s = Bundle.getFileMap ( parent + "?format=json", "Name", l, mCredentials );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
      e.printStackTrace ();
      return -1L;
    }
    if ( s.containsKey ( mName ) ) {
      mContentLength = Long.valueOf ( s.get ( mName ).get ( 1 ) ).longValue ();
      mURL = s.get ( mName ).get ( 0 );
      if ( mURL.startsWith ( "/REST" ) ) {
        mURL = mURL.substring ( 5 );
      }
      logger.debug ( "getContentLength: length: " + mContentLength + " URL: " + mURL );
    } else {
      return -1L;
    }
    return mContentLength;
  }

  public InputStream getContents () throws IOException {
    open ();
    if ( mFile.getFD ().valid () == false ) {
      logger.error ( "File description for " + mPath + " is not valid" );
      throw new IOException ( "Invalid file descriptor for " + mPath );
    }
    try {
      waitForDownload ();
    } catch ( Exception e ) {
      logger.error ( "getContents: Waiting for download failed", e );
      throw new IOException ( e.getLocalizedMessage () );
    }
    logger.debug ( "getContents: sending FileInputStream from file: " + mCachedFile );
    return new FileInputStream ( mCachedFile );
  }

  public void sendContent ( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException {
    if ( getContentLength () == -1L ) {
      throw new NotAuthorizedException ( this );
    }
    try {
      logger.debug ( "sendContent: request for " + mAbsolutePath );
      InputStream in = getContents ();
      logger.debug ( "got contents for " + mAbsolutePath );

      long start = 0, end = mContentLength;
      if ( range != null ) {
        start = range.getStart ();
        end = range.getFinish ();
      }

      if ( mContentLength != mChannel.size () ) {
        logger.error ( "The channel does not have the proper size" );
        throw new IOException ( "The cached file is not the expected size" );
      }

      // Seek to beginning of read
      in.skip ( start );
      long position = start;
      final int bufferSize = 2048;
      byte b[] = new byte[bufferSize];
      while ( position < end ) {
        // How much can we read
        int readSize = (int) Math.min ( end - position, (long) b.length );
        readSize = in.read ( b, 0, readSize );
        if ( readSize == -1 ) {
          throw new IOException ( "Premature end of file" );
        }
        out.write ( b, 0, readSize );
        position += readSize;
      }
      in.close ();
    } catch ( IndexOutOfBoundsException e ) {
      logger.error ( "out of bounds", e );
      throw new IOException ( "error reading" );
    }
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

  void open () throws IOException {
    // see if we have contents in the cache
    logger.debug ( "Opening " + mPath + " from remote URL " + mURL );
    Element n = XNATFS.sContentCache.get ( mAbsolutePath );
    mCachedFile = null;
    if ( n != null ) {
      mCachedFile = (File) n.getObjectValue ();
      logger.debug ( "Found cached file for " + mAbsolutePath + " at " + mCachedFile );
      if ( mCachedFile.exists () && mCachedFile.canRead () ) {
        logger.debug ( "File exists and is readable" );
        mDownloadComplete = true;
      } else {
        // File is bogus
        logger.debug ( "Cached file was not readable or didn't exist" );
        XNATFS.sContentCache.remove ( mAbsolutePath );
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
        HttpClient client = Connection.getInstance ().getClient ( mCredentials );
        HttpGet httpget = new HttpGet ( Connection.getInstance ().formatURL ( mURL ) );
        BasicHttpContext context = new BasicHttpContext ();

        // Generate BASIC scheme object and stick it to the local
        // execution context
        BasicScheme basicAuth = new BasicScheme ();
        context.setAttribute ( "preemptive-auth", basicAuth );
        HttpResponse response = client.execute ( httpget, context );
        logger.debug ( "fetching " + mURL );
        logger.debug ( "Full URL: " + Connection.getInstance ().formatURL ( mURL ) );
        // Try all at once

        int TotalCount = 0;
        int NextReportCount = 1000000;
        HttpEntity entity = response.getEntity ();
        byte[] BackingBuffer = new byte[4096];
        ByteBuffer buffer = ByteBuffer.wrap ( BackingBuffer );
        InputStream in = entity.getContent ();
        logger.debug ( "Fetching remote object " + mURL + " as virtual file " + mPath + " into real file " + mCachedFile );
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
            // logger.debug ( "Writing " + readCount + " bytes to virtual file "
            // + mAbsolutePath + " for URL: " + mURL );
            mChannel.write ( buffer );
          }
          TotalCount += readCount;
          if ( TotalCount > NextReportCount ) {
            NextReportCount += 100000;
            logger.debug ( "Read " + readCount + " ( " + TotalCount + " ) from remote file at " + mAbsolutePath );
          }
        }
        logger.debug ( "Finished fetching " + mURL + " as virtual file " + mAbsolutePath + " into cache file " + mCachedFile );
        mDownloadComplete = true;
        entity.consumeContent ();
        mContentLength = mChannel.size ();
      } catch ( Exception ex ) {
        logger.error ( "Failed to get body of " + mAbsolutePath + " from URL " + mURL, ex );
        return Boolean.FALSE;
      } catch ( Throwable t ) {
        logger.error ( "Caught throwable for " + mAbsolutePath + " URL " + mURL, t );
        return Boolean.FALSE;
      }
      Element n = new Element ( mAbsolutePath, mCachedFile );
      XNATFS.sContentCache.put ( n );
      logger.debug ( "Finished caching " + mAbsolutePath + " from " + mURL );
      return Boolean.TRUE;
    }
  }

}
