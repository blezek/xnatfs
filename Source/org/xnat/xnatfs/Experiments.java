

package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;

import java.io.InputStreamReader;
import java.util.*;
import org.apache.log4j.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.sf.ehcache.constructs.blocking.*;
import net.sf.ehcache.constructs.*;
import net.sf.ehcache.*;
/**
 * Class to handle a users.  Shows up as a directory with three files in it.
 */
public class Experiments extends Container {

  private static final Logger logger = Logger.getLogger(Experiments.class);
  public Experiments ( String path ) {
    super ( path );
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      setter.set(
                 this.hashCode(),
                 FuseFtypeConstants.TYPE_DIR | 0755,
                 0,
                 0, 0, 0,
                 1, 1,
                 time, time, time
                 );
      return 0;
    } 
    return Errno.ENOENT;
  }
  
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      HashSet<String> experiments = getElementList("id");
      for ( String experiment : experiments ) {
        createChild ( experiment );
        filler.add ( experiment, experiment.hashCode(), FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  
  /** Create a child of this node.  Note, the child is a single filename, not a path
   */
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath  );
    HashSet<String> experimentList = getElementList("id");
    if ( experimentList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new Experiment ( childPath, child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    return null;
  }
         
}
