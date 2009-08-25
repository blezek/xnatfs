/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.SecurityManager;
import com.ettrema.http.fs.NullSecurityManager;

/**
 * Main xnatfs-webdav class
 * 
 * @author blezek
 * 
 */
public class XNATFS implements ResourceFactory {
  private static final Logger logger = Logger.getLogger ( XNATFS.class );

  /** Cache services from ehcache */
  public static Cache sNodeCache;
  public static Cache sContentCache;
  public static Cache sFileHandleCache;
  public static CacheManager mMemoryCacheManager;

  /** Thread pool for background downloads of files */
  public static ExecutorService sExecutor = Executors.newCachedThreadPool ();
  public static File sTemporaryDirectory;

  SecurityManager securityManager;

  public XNATFS () {
    // Configure log4j
    // First try to configure from local directory, then from application
    // support directory
    File props;
    props = new File ( System.getProperty ( "user.dir", "." ), "log4j.properties" );
    if ( props.exists () && props.canRead () ) {
      PropertyConfigurator.configure ( props.getAbsolutePath () );
    } else {
      props = new File ( getApplicationResourceDirectory ( "xnatfs" ), "log4j.properties" );
      if ( props.exists () && props.canRead () ) {
        PropertyConfigurator.configure ( props.getAbsolutePath () );
      } else {
        URL url = xnatfsServlet.class.getResource ( "/log4j.properties" );
        if ( url != null ) {
          PropertyConfigurator.configure ( url );
        } else {
          BasicConfigurator.configure ();
        }
      }
    }
    configureCache ();
    configureConnection ();
    sTemporaryDirectory = new File ( System.getProperty ( "java.io.tmpdir" ) );
    if ( mMemoryCacheManager.getDiskStorePath () != null ) {
      sTemporaryDirectory = new File ( mMemoryCacheManager.getDiskStorePath () );
    }
    sTemporaryDirectory = new File ( sTemporaryDirectory, "FileCache" );
    sTemporaryDirectory.mkdirs ();

    Root root = new Root ( this, null, "/" );
    Element e = new Element ( "/", root );
    e.setEternal ( true );
    sNodeCache.put ( e );
    securityManager = new NullSecurityManager ();
  }

  /*
   * Configure the cache by looking up the ehcache.xml file in the system
   * classpath. Expects to have a "Node", "Content" and "FileHandle" cache. If
   * any cache does not exist, it is created on the fly.
   * 
   * First checks the local directory, then the application directory, and
   * finally falls back on the embedded file.
   * 
   * Register a caches listener for the content cache to close and delete files.
   */
  static public void configureCache () {
    URL url = ClassLoader.getSystemResource ( "ehcache.xml" );
    logger.info ( "Found configuration URL: " + url );
    File SystemConfig = new File ( getApplicationResourceDirectory ( "xnatfs" ), "ehcache.xml" );
    File LocalConfig = new File ( System.getProperty ( "user.dir", "." ), "ehcache.xml" );
    if ( LocalConfig.exists () && LocalConfig.canRead () ) {
      mMemoryCacheManager = CacheManager.create ( LocalConfig.getAbsolutePath () );
    } else if ( SystemConfig.exists () && SystemConfig.canRead () ) {
      mMemoryCacheManager = CacheManager.create ( SystemConfig.getAbsolutePath () );
    } else if ( url != null ) {
      mMemoryCacheManager = CacheManager.create ( url );
    } else {
      mMemoryCacheManager = CacheManager.create ();
    }

    if ( mMemoryCacheManager.getCache ( "Node" ) == null ) {
      mMemoryCacheManager.addCache ( "Node" );
    }
    if ( mMemoryCacheManager.getCache ( "Content" ) == null ) {
      mMemoryCacheManager.addCache ( "Content" );
    }
    sNodeCache = mMemoryCacheManager.getCache ( "Node" );
    sContentCache = mMemoryCacheManager.getCache ( "Content" );
    // sContentCache.getCacheEventNotificationService ().registerListener ( new
    // CacheFileCleanup () );
    sFileHandleCache = mMemoryCacheManager.getCache ( "FileHandle" );
    if ( sNodeCache == null ) {
      logger.error ( "Failed to create filecache" );
    }
  }

  /*
   * Configure the connection. Hard coded username and password for now.
   */
  static public void configureConnection () {
    XNATConnection.getInstance ().setUsername ( "blezek" );
    XNATConnection.getInstance ().setPassword ( "throwaway" );
  }

  /**
   * Returns the Node indicated by the path argument. If the note is not in the
   * xnatfs.sNodeCache, call createChild to try to create the parent and this
   * child within it.
   * 
   * @see com.bradmcevoy.http.ResourceFactory#getResource(java.lang.String,
   *      java.lang.String)
   */
  public Resource getResource ( String host, String path ) {
    logger.debug ( "getResource: " + host + " path: " + path );
    path = path.replaceAll ( "/xnatfs", "" );
    Element element = XNATFS.sNodeCache.get ( path );
    if ( element != null ) {
      return (Resource) element.getObjectValue ();
    }
    logger.debug ( "Couldn't find node '" + path + "', trying to create the file in the parent" );
    return createChild ( VirtualResource.dirname ( path ), VirtualResource.tail ( path ) );
  }

  /**
   * Lookup the path, if it exists, try to create the child. If the Node
   * indicated by the path is not in the cache, try to create it by looking up
   * the parent by calling this function recursively.
   * 
   * @param path
   *          Path to the node in which to create child
   * @param child
   *          Name of the Node to create
   * @return The new Node. Should never return null.
   */
  synchronized Resource createChild ( String path, String child ) {
    logger.debug ( "createChild: " + path + " child: " + child );
    if ( path.equals ( "/" ) && child.equals ( "" ) ) {
      return null;
    }
    Element element = XNATFS.sNodeCache.get ( path );
    VirtualDirectory parent = null;
    Resource r = null;
    if ( element != null ) {
      logger.debug ( "found parent " + element.getObjectValue () );
      r = (Resource) element.getObjectValue ();
    } else {
      logger.debug ( "Didn't find parent, attempting to create" );
      r = createChild ( VirtualResource.dirname ( path ), VirtualResource.tail ( path ) );
    }
    if ( r != null && r instanceof VirtualResource ) {
      parent = (VirtualDirectory) r;
    }
    if ( parent == null ) {
      logger.error ( "Couldn't find parent of " + path + child );
      return null;
    }
    try {
      return parent.child ( child );
    } catch ( Exception e ) {
      logger.error ( "Falied to create child: " + child, e );
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.bradmcevoy.http.ResourceFactory#getSupportedLevels()
   */
  public String getSupportedLevels () {
    return "1,2";
  }

  /**
   * @return
   */
  public SecurityManager getSecurityManager () {
    return securityManager;
  }

  /**
   * @return
   */
  public String getRealm () {
    return securityManager.getRealm ();
  }

  /**
   * Returns the appropriate working directory for storing application data. The
   * result of this method is platform dependant: On linux, it will return
   * ~/applicationName, on windows, the working directory will be located in the
   * user's application data folder. For Mac OS systems, the working directory
   * will be placed in the proper location in "Library/Application Support".
   * <p/>
   * This method will also make sure that the working directory exists. When
   * invoked, the directory and all required subfolders will be created.
   * 
   * @param applicationName
   *          Name of the application, used to determine the working directory.
   * @return the appropriate working directory for storing application data.
   */
  public static File getApplicationResourceDirectory ( final String applicationName ) {
    final String userHome = System.getProperty ( "user.home", "." );
    final File workingDirectory;
    switch ( getOS () ) {
    case UNIX:
      workingDirectory = new File ( userHome, '.' + applicationName + '/' );
      break;
    case WINDOWS:
      final String applicationData = System.getenv ( "APPDATA" );
      if ( applicationData != null )
        workingDirectory = new File ( applicationData, "." + applicationName + '/' );
      else
        workingDirectory = new File ( userHome, '.' + applicationName + '/' );
      break;
    case MACINTOSH:
      workingDirectory = new File ( userHome, "Library/Application Support/" + applicationName );
      break;
    default:
      return new File ( "." );
    }
    if ( !workingDirectory.exists () && !workingDirectory.mkdirs () ) {
      throw new RuntimeException ( "The working directory could not be created: " + workingDirectory );

    }
    return workingDirectory;
  }

  public enum OSType {
    WINDOWS, MACINTOSH, UNIX
  }

  static OSType getOS () {
    String sysName = System.getProperty ( "os.name" ).toLowerCase ();
    if ( sysName.contains ( "windows" ) ) {
      return OSType.WINDOWS;
    } else if ( sysName.contains ( "mac" ) ) {
      return OSType.MACINTOSH;
    } else {
      return OSType.UNIX;
    }
  }
}