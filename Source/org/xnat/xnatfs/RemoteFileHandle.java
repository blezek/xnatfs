package org.xnat.xnatfs;

import org.apache.log4j.*;
import fuse.compat.*;
import fuse.*;
import java.util.*;

import org.json.*;
import java.io.IOException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.*;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;

import fuse.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.httpclient.util.*;

public class RemoteFileHandle {

  int mBufferSize;
  public long mLocation;
  public long mLength;
  GetMethod mGet;
  InputStream mStream;
  String mPath;
  String mURL;

  public RemoteFileHandle ( String url, String path ) {
    mURL = url;
    mPath = path;
    mGet = null;
    mLocation = 0;
    mBufferSize = 4096;
    mLength = -1;

    // see if we have contents in the cache
    Element n = xnatfs.sContentCache.get ( mPath );
    if ( n != null ) {
      byte[] contents = (byte[]) n.getObjectValue();
      mLength = contents.length;
    }
    mStream = null;
  }

  byte[] checkCacheForContents() {
    Element n = xnatfs.sContentCache.get ( mPath );
    if ( n != null ) {
      byte[] contents = (byte[]) n.getObjectValue();
      mLength = contents.length;
      return contents;
    }
    return null;
  }
  
  public void release() {
    if ( mGet != null ) {
      mGet.releaseConnection();
    }
  }
  
  // public GetMethod getGet() { return mGet; }

  public InputStream getStream ( int bufferSize ) throws Exception {
    mBufferSize = bufferSize;
    return getStream ( );
  }
  
  public InputStream getStream ( ) throws Exception {
    byte[] b = checkCacheForContents();
    if ( b != null ) {
      mStream = new ByteArrayInputStream ( getBytes() );
    }
    if ( mStream == null ) {
      open();
      mStream = new BufferedInputStream ( mGet.getResponseBodyAsStream(), mBufferSize );
      // Put a mark at the beginning
      mStream.mark ( mBufferSize );
    }
    return mStream;
  }

  void open() throws Exception {
    if ( mGet == null ) {
      mGet = new GetMethod ( mURL );
     XNATConnection.getInstance().getClient().executeMethod ( mGet );
    }
  }

  
  public byte[] getBytes() throws Exception {
    byte[] b;
    Element n = xnatfs.sContentCache.get ( mPath );
    if ( n != null ) {
      b = (byte[]) n.getObjectValue();
    } else {
      open();
      b = mGet.getResponseBody();
      n = new Element ( mPath, b );	
      xnatfs.sContentCache.put ( n );
    }      
    mLocation = mLocation + b.length;
    return b;
  }
}
