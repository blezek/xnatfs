/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bradmcevoy.http.GetHandler;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.MsOfficeResponseHandler;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.ResponseHandler;
import com.ettrema.berry.Berry;
import com.ettrema.berry.HttpAdapter;
import com.ettrema.berry.xlightweb.XLightWebAdapter;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.defaultsources.PropertyDefaultSource;

/**
 * @author blezek
 * 
 */
public class StartServer {

  /**
   * @param args
   */
  public static void main ( String[] args ) {

    JSAPResult config = parseArguments ( args );

    Connection.getInstance ().setUsername ( config.getString ( "username" ) );
    Connection.getInstance ().setPassword ( config.getString ( "password" ) );
    Connection.getInstance ().setHost ( config.getString ( "server" ) );
    Connection.getInstance ().setPort ( Integer.toString ( config.getInt ( "port" ) ) );

    ResourceFactory resourceFactory = new XNATFS ();
    ResponseHandler handler = new MsOfficeResponseHandler ();
    HttpManager httpManager = new HttpManager ( resourceFactory, handler ) {
      protected GetHandler createGetHandler () {
        return new GetHandlerLocal ( this );
      }
    };
    List<HttpAdapter> httpAdapters = new ArrayList<HttpAdapter> ();
    httpAdapters.add ( new XLightWebAdapter ( config.getInt ( "serverport" ) ) );
    @SuppressWarnings("unused")
    Berry berry = new Berry ( httpManager, httpAdapters );

    // berry is now running...
    // berry.stop ();

  }

  @SuppressWarnings("unchecked")
  static JSAPResult parseArguments ( String[] args ) {
    JSAPResult config = null;
    try {
      SimpleJSAP jsap = new SimpleJSAP ( "xnatfs", "xnatfs is a WebDAV server.  xnatfs allows "
          + " local mounting of an XNAT instance.  Users of the system can browse projects, experiments, subjects and data using the " + " a WebDAV filesystem, rather than the web interface." );

      FlaggedOption username = new FlaggedOption ( "username" ).setStringParser ( JSAP.STRING_PARSER ).setDefault ( JSAP.NO_DEFAULT ).setRequired ( true ).setShortFlag ( 'u' ).setLongFlag (
          "username" );
      jsap.registerParameter ( username );
      username.setHelp ( "XNAT username for connection to the XNAT server." );

      FlaggedOption password = new FlaggedOption ( "password" ).setStringParser ( JSAP.STRING_PARSER ).setDefault ( JSAP.NO_DEFAULT ).setRequired ( true ).setShortFlag ( 'p' ).setLongFlag (
          "password" );
      jsap.registerParameter ( password );
      password.setHelp ( "Password for XNAT username for connection to the XNAT server." );

      FlaggedOption server = new FlaggedOption ( "server" ).setStringParser ( JSAP.STRING_PARSER ).setDefault ( "central.xnat.org" ).setRequired ( true ).setShortFlag ( 's' ).setLongFlag ( "server" );
      jsap.registerParameter ( server );
      server.setHelp ( "XNAT server.  Should be in the form of a web site address, i.e. central.xnat.org." );

      FlaggedOption port = new FlaggedOption ( "port" ).setStringParser ( JSAP.INTEGER_PARSER ).setDefault ( "80" ).setRequired ( false ).setShortFlag ( 'o' ).setLongFlag ( "port" );
      jsap.registerParameter ( port );
      port.setHelp ( "port to use on the XNAT server, defaults to 80" );

      // Get a list of remaining options
      UnflaggedOption serverPort = new UnflaggedOption ( "serverport" ).setStringParser ( JSAP.INTEGER_PARSER ).setDefault ( "8081" ).setRequired ( false ).setGreedy ( false );
      serverPort.setHelp ( "Port number to run the local server on, default is 8081" );
      jsap.registerParameter ( serverPort );

      // Register the default sources. First in the application directory, next
      // in the local directory
      File defaultsFile = new File ( XNATFS.getApplicationResourceDirectory ( "xnatfs" ), "xnatfs.props" );
      jsap.registerDefaultSource ( new PropertyDefaultSource ( defaultsFile.getAbsolutePath (), false ) );
      defaultsFile = new File ( System.getProperty ( "user.dir", "." ), "xnatfs.props" );
      jsap.registerDefaultSource ( new PropertyDefaultSource ( defaultsFile.getAbsolutePath (), false ) );

      config = jsap.parse ( args );

      if ( !config.success () ) {

        System.err.println ();

        // print out specific error messages describing the problems
        // with the command line, THEN print usage, THEN print full
        // help. This is called "beating the user with a clue stick."
        System.err.println ( "The following errors were found while parsing the command line:" );
        for ( java.util.Iterator errs = config.getErrorMessageIterator (); errs.hasNext (); ) {
          System.err.println ( "Error: " + errs.next () );
        }

        System.err.println ();
        System.err.println ( "Usage: java " + StartServer.class.getName () );
        System.err.println ( "                " + jsap.getUsage () );
        System.err.println ();
        System.err.println ( jsap.getHelp () );
        System.exit ( 1 );
      }
    } catch ( Exception e ) {
      System.err.println ( "parsing command line " + e );
      System.exit ( 1 );
    }
    if ( config.userSpecified ( "help" ) ) {
      System.exit ( 0 );
    }
    return config;
  }

}
