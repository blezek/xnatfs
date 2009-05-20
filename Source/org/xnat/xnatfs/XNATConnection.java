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
import java.util.concurrent.ConcurrentHashMap;

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

  public HttpClient getClient() { return mClient; }

  /** Return a RemoteFileHandle corresponding to the url.  The caller needs to process
   * then call release on the RometFileHandle.
   */
  public RemoteFileHandle get ( String s, String path ) throws Exception {
    String URL = "http://" + mHost + ":" + mPort + mPrefix + s;
    logger.debug ( "Trying to get: " + URL );
    // GetMethod get = new GetMethod ( URL );
    // mClient.executeMethod ( get );
    return new RemoteFileHandle ( URL, path );
  }

  static String mHost;
  static String mPort;
  static String mPrefix;
  static String mPassword;
  static String mUsername;
  HttpClient mClient;
  Credentials mCredentials;

  protected void setup() {
    mCredentials = new UsernamePasswordCredentials ( mUsername, mPassword );
    mClient.getState().setCredentials ( new AuthScope ( mHost, -1 ), mCredentials );
    mClient.getParams().setAuthenticationPreemptive ( true );
  }

  protected XNATConnection () {
    mClient = new HttpClient ( new MultiThreadedHttpConnectionManager() );
    mHost = "central.xnat.org";
    mUsername = "guest";
    mPassword = "guest";
    mPrefix = "/REST";
    mPort = "80";
    setup();
  }
}
