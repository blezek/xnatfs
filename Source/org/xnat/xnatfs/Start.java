/**
 * 
 */
package org.xnat.xnatfs;

import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.martiansoftware.jsap.*;

import fuse.FuseMount;

/**
 * @author blezek
 * 
 */
public class Start {
  private static final Logger logger = Logger.getLogger ( Start.class );

  /**
   * Startup the xnatfs Fuse filesystem. Configure some logging information,
   * cache, and connection then startup fuse.
   * 
   * @param args
   *          arguments to be parsed
   */
  public static void main ( String[] args ) {
    Logger.getLogger ( "org.xnat.xnatfs" ).setLevel ( Level.DEBUG );
    Logger.getLogger ( "org.apache.commons" ).setLevel ( Level.WARN );
    Logger.getLogger ( "httpclient.wire" ).setLevel ( Level.WARN );
    Logger.getLogger ( "org.apache.http" ).setLevel ( Level.WARN );
    logger.info ( "Starting xnatfs" );
    xnatfs.configureCache ();
    xnatfs.configureConnection ();
    JSAPResult config = parseArguments ( args );

    XNATConnection.getInstance ().setUsername ( config.getString ( "username" ) );
    XNATConnection.getInstance ().setPassword ( config.getString ( "password" ) );
    XNATConnection.getInstance ().setPort ( Integer.toString ( config.getInt ( "port" ) ) );

    try {
      Log l = LogFactory.getLog ( "org.xnat.xnatfs.FuseMount" );
      FuseMount.mount ( config.getStringArray ( "fuseopts" ), new xnatfs (), l );
    } catch ( Exception e ) {
      e.printStackTrace ();
    } finally {
      logger.info ( "shutting down" );
      CacheManager.getInstance ().shutdown ();
      // Shutdown the executor
      xnatfs.sExecutor.shutdown (); // Disable new tasks from being submitted
      logger.info ( "Shutdown threadpool" );
      try {
        // Wait a while for existing tasks to terminate
        logger.info ( "Waiting for termination" );
        if ( !xnatfs.sExecutor.awaitTermination ( 10, TimeUnit.SECONDS ) ) {
          logger.info ( "Forcing shutdown of threadpool" );
          xnatfs.sExecutor.shutdownNow (); // Cancel currently executing tasks
          // Wait a while for tasks to respond to being cancelled
          if ( !xnatfs.sExecutor.awaitTermination ( 10, TimeUnit.SECONDS ) )
            logger.error ( "Pool did not terminate" );
        }
      } catch ( InterruptedException ie ) {
        // (Re-)Cancel if current thread also interrupted
        xnatfs.sExecutor.shutdownNow ();
        // Preserve interrupt status
        Thread.currentThread ().interrupt ();
      }
      xnatfs.sExecutor.shutdownNow ();
      logger.info ( "exited" );
    }
  }

  static JSAPResult parseArguments ( String[] args ) {
    JSAPResult config = null;
    try {
      SimpleJSAP jsap = new SimpleJSAP ( "xnatfs", "xnatfs is a user level file system using Fuse and fusej4.  xnatfs allows "
          + " local mounting of an XNAT instance.  Users of the system can browse projects, experiments, subjects and data using the " + " local filesystem, rather than the web interface." );

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
      UnflaggedOption fuseopts = new UnflaggedOption ( "fuseopts" ).setStringParser ( JSAP.STRING_PARSER ).setRequired ( true ).setGreedy ( true );
      fuseopts.setHelp ( "Options to FUSE of the form <mountpoint> [options].\n" + "\t<mountpoint> is a directory where the FUSE filessystem should be mounted\n" + "Useful options are: \n"
          + "\t-f -- don't fork into the background\n" + "\t-ovolname=NAME -- show the filesystems as NAME to the OS\n"
          + "\t-oauto_xattr -- don't forward xattr calls to xnatfs, without this option things may not work correctly\n"
          + "\tvolicon=PATH, where PATH is path to an icon (.icns) file, allows a icon for the filesystem\n"
          + "\t-ofsname=NAME, where NAME is a string. This option can be used to specify the \"file system name\", analogous to the device in the case of a disk-backed \"real\" file system."
          + "\nMore help can be found at http://code.google.com/p/macfuse/wiki/OPTIONS" );
      jsap.registerParameter ( fuseopts );

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
        System.err.println ( "Usage: java " + Start.class.getName () );
        System.err.println ( "                " + jsap.getUsage () );
        System.err.println ();
        System.err.println ( jsap.getHelp () );
        System.exit ( 1 );
      }
      // System.exit ( 0 );
    } catch ( Exception e ) {
      logger.error ( "parsing command line", e );
    }
    return config;
  }
}
