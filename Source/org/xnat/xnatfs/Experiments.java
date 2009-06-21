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
 * Container holding multiple <code>Experiment</code> objects.
 * 
 * @author blezek
 * 
 */
public class Experiments extends Container {

  private static final Logger logger = Logger.getLogger ( Experiments.class );

  public Experiments ( String path ) {
    super ( path );
  }

  /**
   * Get the attributes of this directory.
   * 
   * @see org.xnat.xnatfs.Node#getattr(java.lang.String, fuse.FuseGetattrSetter)
   */
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis () / 1000L);
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, time, time, time );
      return 0;
    }
    return Errno.ENOENT;
  }

  /**
   * Fill in the directory structure for this directory. Adds a directory for
   * each <code>Experiment</code> referenced by this object.
   * 
   * @see org.xnat.xnatfs.Container#getdir(java.lang.String, fuse.FuseDirFiller)
   */
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      HashSet<String> experiments = getElementList ();
      for ( String experiment : experiments ) {
        createChild ( experiment );
        filler.add ( experiment, experiment.hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  /**
   * Create a child object. All the children are Experiment objects.
   * 
   * @see Experiment
   * @see org.xnat.xnatfs.Container#createChild(java.lang.String)
   */
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath );
    HashSet<String> experimentList = getElementList ();
    if ( experimentList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new Experiment ( childPath, child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return null;
  }

}
