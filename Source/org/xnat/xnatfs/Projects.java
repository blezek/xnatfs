

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
public class Projects extends Node {

  private static final Logger logger = Logger.getLogger(Projects.class);
  public Projects ( String path ) {
    super ( path );
  }

  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    int time = (int) (System.currentTimeMillis() / 1000L);
    if ( path.equals ( mPath ) ) {
      setter.set(
                 this.hashCode(),
                 FuseFtypeConstants.TYPE_DIR | 0755,
                 3,
                 0, 0, 0,
                 -1, -1,
                 time, time, time
                 );
      return 0;
    } 
    return Errno.ENOENT;
  }
  
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      HashSet<String> projectList = getProjectList();
      for ( String project : projectList ) {
        createChild ( project );
        filler.add ( project,
                     project.hashCode(),
                     FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  HashSet<String> getProjectList () throws FuseException {
    // Get the projects code
    Element element = xnatfs.sContentCache.get ( mPath );
    HashSet<String> projectList = null;
    if ( element == null ) {
      try {
        projectList = new HashSet<String>();
        InputStreamReader reader = new InputStreamReader ( XNATConnection.getInstance().getURLAsStream ( mPath + "?format=json" ) );
        JSONTokener tokenizer = new JSONTokener ( reader );
        JSONObject json = new JSONObject ( tokenizer );
        JSONArray projects = json.getJSONObject ( "ResultSet" ).getJSONArray ( "Result" );
        for ( int idx = 0; idx < projects.length(); idx++ ) {
          if ( projects.isNull ( idx ) ) { continue; }
          String id = projects.getJSONObject ( idx ).getString ( "id" );
          projectList.add ( id );
        }
      } catch ( Exception e ) {
        logger.error ( "Caught exception reading " + mPath, e );
        throw new FuseException();
      }
      element = new Element ( mPath, projectList );
      xnatfs.sContentCache.put ( element );
    }
    projectList = (HashSet<String>) element.getObjectValue();
    return projectList;
  }

  /** Create a child of this node.  Note, the child is a single filename, not a path
   */
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath  );
    HashSet<String> projectList = getProjectList();
    if ( projectList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new Project ( childPath, child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    return null;
  }
         
}
