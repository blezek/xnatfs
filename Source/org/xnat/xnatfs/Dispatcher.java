package org.xnat.xnatfs;

import net.sf.ehcache.Element;

import org.apache.log4j.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Dispatcher {

  private static final Logger logger = Logger.getLogger(Dispatcher.class);
  static Dispatcher sDispatcher = new Dispatcher();

  public Dispatcher getInstance () { return sDispatcher; }

  static Node createChild ( String path, String child ) {
    logger.debug ( "createChild: " + path + " child: " + child );
    if ( path.equals ( "/" ) && child.equals ( "" ) ) { return null; }
    Element element = xnatfs.sNodeCache.get ( path );
    Node parent = null;
    if ( element != null ) {
      logger.debug ( "found parent" );
      parent = (Node)element.getObjectValue(); 
    } else {
      logger.debug ( "Didn't find parent, attempting to create" );
      parent = createChild ( Node.root ( path ), Node.tail ( path ) );
    }
    if ( parent == null ) {
      return null;
    }
    return parent.createChild ( child );
  }

  static public Node getNode ( String path ) {
    Element element = xnatfs.sNodeCache.get ( path );
    if ( element != null ) {
      return (Node)element.getObjectValue();
    }
    logger.debug ( "Couldn't find node '" + path + "', trying to create the file in the parent" );
    return createChild ( Node.root ( path ), Node.tail ( path ) );
  }


  Dispatcher() {
  }
}
