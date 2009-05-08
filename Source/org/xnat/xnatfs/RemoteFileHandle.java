package org.xnat.xnatfs;

import org.apache.log4j.*;
import fuse.compat.*;
import fuse.*;
import java.util.*;

import org.json.*;

import org.jdom.*;
import org.jdom.input.*;
import java.io.IOException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.CharBuffer;
import java.util.*;

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
  BufferedInputStream mStream;

  public RemoteFileHandle ( GetMethod get ) {
    mGet = get;
    mLocation = 0;
    mBufferSize = 4096;
    mLength = mGet.getResponseContentLength();
    mStream = null;
  }
  
  public void release() {
    if ( mGet != null ) {
      mGet.releaseConnection();
    }
  }
  
  public GetMethod getGet() { return mGet; }

  public InputStream getStream ( int bufferSize ) throws Exception {
    mBufferSize = bufferSize;
    return getStream ( );
  }
  
  public InputStream getStream ( ) throws Exception {
	  if ( mStream == null ) {
    mStream = new BufferedInputStream ( mGet.getResponseBodyAsStream(), mBufferSize );
    // Put a mark at the beginning
    mStream.mark ( mBufferSize );
	  }
    return mStream;
  }
  
  public byte[] getBytes() throws Exception {
    byte[] b = mGet.getResponseBody();
    mLocation = mLocation + b.length;
    return b;
  }
}
