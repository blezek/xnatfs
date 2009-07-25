package org.xnat.xnatfs;

import net.sf.ehcache.Element;

import org.apache.log4j.*;
import fuse.compat.*;
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public class XNATConnection {
  private static final Logger logger = Logger.getLogger ( XNATConnection.class );

  static public XNATConnection getInstance () {
    return sInstance;
  }

  static private XNATConnection sInstance = new XNATConnection ();

  public void setUsername ( String s ) {
    mUsername = s;
    setup ();
  }

  public void setPassword ( String s ) {
    mPassword = s;
    setup ();
  }

  public void setHost ( String s ) {
    mHost = s;
    setup ();
  }

  public void setPort ( String s ) {
    mPort = s;
    setup ();
  }

  public HttpClient getClient () {
    DefaultHttpClient client = new DefaultHttpClient ( cm, params );
    client.getCredentialsProvider ().setCredentials ( new AuthScope ( AuthScope.ANY_HOST, AuthScope.ANY_PORT ), new UsernamePasswordCredentials ( mUsername, mPassword ) );

    BasicHttpContext localcontext = new BasicHttpContext ();

    // Generate BASIC scheme object and stick it to the local
    // execution context
    BasicScheme basicAuth = new BasicScheme ();
    localcontext.setAttribute ( "preemptive-auth", basicAuth );

    // Add as the first request interceptor
    client.addRequestInterceptor ( new PreemptiveAuth (), 0 );
    return client;

  }

  /**
   * Return a RemoteFileHandle corresponding to the url. The caller needs to
   * process then call release on the RemoteFileHandle.
   */
  synchronized public RemoteFileHandle get ( String s, String path ) throws Exception {
    String URL = "http://" + mHost + ":" + mPort + mPrefix + s;
    logger.debug ( "Trying to get: " + URL );
    // GetMethod get = new GetMethod ( URL );
    // mClient.executeMethod ( get );
    return new RemoteFileHandle ( URL, path );
  }

  synchronized public FileHandle getFileHandle ( String s, String path ) throws Exception {
    String URL = "http://" + mHost + ":" + mPort + mPrefix + s;
    logger.debug ( "Trying to get: " + URL );
    Element n = xnatfs.sFileHandleCache.get ( path );
    if ( n != null ) {
      return (FileHandle) n.getObjectValue ();
    }
    FileHandle fh = new FileHandle ( URL, path );
    n = new Element ( path, fh );
    xnatfs.sFileHandleCache.put ( n );
    return fh;
  }

  String mHost = "central.xnat.org";
  String mUsername = "guest";
  String mPassword = "guest";
  String mPrefix = "/REST";
  String mPort = "80";
  HttpClient mClient;
  Credentials mCredentials;

  protected void setup () {
    // mCredentials = new UsernamePasswordCredentials ( mUsername, mPassword );
    // mClient.getState ().setCredentials ( new AuthScope ( mHost, -1 ),
    // mCredentials );
    // mClient.getParams ().setAuthenticationPreemptive ( true );
  }

  HttpParams params;
  SchemeRegistry schemeRegistry;
  ClientConnectionManager cm;

  protected XNATConnection () {
    // mClient = new HttpClient ( new MultiThreadedHttpConnectionManager () );

    params = new BasicHttpParams ();
    ConnManagerParams.setMaxTotalConnections ( params, 100 );
    HttpProtocolParams.setVersion ( params, HttpVersion.HTTP_1_1 );

    // Create and initialize scheme registry
    schemeRegistry = new SchemeRegistry ();
    schemeRegistry.register ( new Scheme ( "http", PlainSocketFactory.getSocketFactory (), 80 ) );

    // Create an HttpClient with the ThreadSafeClientConnManager.
    // This connection manager must be used if more than one thread will
    // be using the HttpClient.
    cm = new ThreadSafeClientConnManager ( params, schemeRegistry );
    // HttpClient httpClient = new DefaultHttpClient ( cm, params );

  }

  static class PreemptiveAuth implements HttpRequestInterceptor {

    public void process ( final HttpRequest request, final HttpContext context ) throws HttpException, IOException {

      AuthState authState = (AuthState) context.getAttribute ( ClientContext.TARGET_AUTH_STATE );

      // If no auth scheme avaialble yet, try to initialize it preemptively
      if ( authState.getAuthScheme () == null ) {
        AuthScheme authScheme = (AuthScheme) context.getAttribute ( "preemptive-auth" );
        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute ( ClientContext.CREDS_PROVIDER );
        HttpHost targetHost = (HttpHost) context.getAttribute ( ExecutionContext.HTTP_TARGET_HOST );
        if ( authScheme != null ) {
          Credentials creds = credsProvider.getCredentials ( new AuthScope ( targetHost.getHostName (), targetHost.getPort () ) );
          if ( creds == null ) {
            throw new HttpException ( "No credentials for preemptive authentication" );
          }
          authState.setAuthScheme ( authScheme );
          authState.setCredentials ( creds );
        }
      }

    }
  }
}
