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


public class XNATConnection {
  private static final Logger logger = Logger.getLogger(XNATConnection.class);

  static public XNATConnection getInstance() {
    return sInstance;
  }

  static private XNATConnection sInstance = new XNATConnection();

  public void setUsername ( String s ) { 
    mUsername = s;
    setup();
  }
  public void setPassword ( String s ) {
    mPassword = s;
    setup();
  }

  public void setHost ( String s ) {
    mHost = s;
    setup();
  }

  public void setPort ( String s ) {
    mPort = s;
    setup();
  }

  /** Get the contents of the url as a stream.  The caller needs to read the stream in it's entirity
   * then close the stream.
   */
  public InputStream getURLAsStream ( String s ) throws Exception {
    if ( mGet != null ) {
      mGet.releaseConnection();
    }
    String URL = "http://" + mHost + ":" + mPort + mPrefix + s;
    logger.debug ( "Trying to get: " + URL );
    mGet = new GetMethod ( URL );
    mClient.executeMethod ( mGet );
    return mGet.getResponseBodyAsStream();
  }

  /** Get the contents of the url as a stream.  The caller needs to read the stream in it's entirity
   * then close the stream.
   */
  public byte[] getURLAsBytes ( String s ) throws Exception {
    if ( mGet != null ) {
      mGet.releaseConnection();
    }
    String URL = "http://" + mHost + ":" + mPort + mPrefix + s;
    logger.debug ( "Trying to get: " + URL );
    mGet = new GetMethod ( URL );
    mClient.executeMethod ( mGet );
    byte[] ret = mGet.getResponseBody();
    mGet.releaseConnection();
    mGet = null;
    return ret;
  }

  String mHost;
  String mPort;
  String mPrefix;
  String mPassword;
  String mUsername;
  HttpClient mClient;
  GetMethod mGet;
  Credentials mCredentials;

  protected void setup() {
    mCredentials = new UsernamePasswordCredentials ( mUsername, mPassword );
    mClient.getState().setCredentials ( new AuthScope ( mHost, -1 ), mCredentials );
    mClient.getParams().setAuthenticationPreemptive ( true );
  }

  protected XNATConnection () {
    mClient = new HttpClient();
    mHost = "central.xnat.org";
    mUsername = "guest";
    mPassword = "guest";
    mPrefix = "/REST";
    mPort = "80";
    mGet = null;
    setup();
  }
}
