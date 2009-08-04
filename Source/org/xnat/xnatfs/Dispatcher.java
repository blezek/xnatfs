package org.xnat.xnatfs;

import net.sf.ehcache.Element;

import org.apache.log4j.*;
import fuse.FuseException;

/**
 * Handles finding Nodes by looking them up in the cache. If a Node is not found
 * in the cache, recursively lookup the parent and ask the parent to create the
 * child.
 * 
 * @author blezek
 * 
 */
public class Dispatcher {

  private static final Logger logger = Logger.getLogger ( Dispatcher.class );
  static Dispatcher sDispatcher = new Dispatcher ();

  public Dispatcher getInstance () {
    return sDispatcher;
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
  static synchronized Node createChild ( String path, String child ) {
    logger.debug ( "createChild: " + path + " child: " + child );
    if ( path.equals ( "/" ) && child.equals ( "" ) ) {
      return null;
    }
    Element element = xnatfs.sNodeCache.get ( path );
    Node parent = null;
    if ( element != null ) {
      logger.debug ( "found parent" );
      parent = (Node) element.getObjectValue ();
    } else {
      logger.debug ( "Didn't find parent, attempting to create" );
      parent = createChild ( Node.dirname ( path ), Node.tail ( path ) );
    }
    if ( parent == null ) {
      return null;
    }
    try {
      return parent.createChild ( child );
    } catch ( FuseException e ) {
      logger.error ( "Falied to create child: " + child, e );
    }
    return null;
  }

  /**
   * Returns the Node indicated by the path argument. If the note is not in the
   * xnatfs.sNodeCache, call createChild to try to create the parent and this
   * child within it.
   */
  static synchronized public Node getNode ( String path ) {
    Element element = xnatfs.sNodeCache.get ( path );
    if ( element != null ) {
      return (Node) element.getObjectValue ();
    }
    logger.debug ( "Couldn't find node '" + path + "', trying to create the file in the parent" );
    return createChild ( Node.dirname ( path ), Node.tail ( path ) );
  }

  Dispatcher () {
  }
}
