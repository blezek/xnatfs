package org.xnat.xnatfs.webdav;

import java.io.IOException;

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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import com.bradmcevoy.http.Auth;

public class Connection {
  private static final Logger logger = Logger.getLogger ( Connection.class );

  String mHost = "central.xnat.org";
  String mUsername = "blezek";
  String mPassword = "throwaway";
  String mPrefix = "/REST";
  String mPort = "80";
  Credentials mCredentials;

  HttpParams params;
  SchemeRegistry schemeRegistry;
  ClientConnectionManager cm;

  static public Connection getInstance () {
    return sInstance;
  }

  static private Connection sInstance = new Connection ();

  void setCredentials () {
  }

  public void setUsername ( String s ) {
    mUsername = s;
    setCredentials ();
  }

  public void setPassword ( String s ) {
    mPassword = s;
    setCredentials ();
  }

  public void setHost ( String s ) {
    mHost = s;
  }

  public void setPort ( String s ) {
    mPort = s;
  }

  public HttpClient getClient ( Auth credentials ) {
    DefaultHttpClient mClient;
    mClient = new DefaultHttpClient ( cm, params );
    logger.debug ( "getClient: credentials are " + credentials.user + "/" + credentials.password );
    mClient.getCredentialsProvider ().setCredentials ( new AuthScope ( AuthScope.ANY_HOST, AuthScope.ANY_PORT ), new UsernamePasswordCredentials ( credentials.user, credentials.password ) );
    /*
     * BasicHttpContext localcontext = new BasicHttpContext ();
     * 
     * // Generate BASIC scheme object and stick it to the local // execution
     * context BasicScheme basicAuth = new BasicScheme ();
     * localcontext.setAttribute ( "preemptive-auth", basicAuth );
     */
    // Add as the first request interceptor
    mClient.addRequestInterceptor ( new PreemptiveAuth (), 0 );
    return mClient;
  }

  public String formatURL ( String path ) {
    String s = "http://" + mHost + ":" + mPort + mPrefix + path;
    return s;
  }

  protected Connection () {
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
  }

  public HttpEntity getEntity ( String url, Auth credentials ) throws Exception {
    HttpClient client = Connection.getInstance ().getClient ( credentials );
    HttpGet httpget = new HttpGet ( Connection.getInstance ().formatURL ( url ) );
    BasicHttpContext context = new BasicHttpContext ();

    // Generate BASIC scheme object and stick it to the local
    // execution context
    BasicScheme basicAuth = new BasicScheme ();
    context.setAttribute ( "preemptive-auth", basicAuth );
    HttpResponse response = client.execute ( httpget, context );
    logger.debug ( "Get entity for: " + Connection.getInstance ().formatURL ( url ) );
    // Try all at once
    return response.getEntity ();
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
