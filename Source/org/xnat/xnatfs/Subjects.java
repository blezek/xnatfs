

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
public class Subjects extends Node {

  private static final Logger logger = Logger.getLogger(Subjects.class);
  public Subjects ( String path ) {
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
      HashSet<String> subjectList = getSubjectList();
      for ( String subject : subjectList ) {
        createChild ( subject );
        filler.add ( subject,
                     subject.hashCode(),
                     FuseFtypeConstants.TYPE_FILE | 0444 );
      }
      return 0;
    }
    return Errno.ENOTDIR;
  }

  HashSet<String> getSubjectList () throws FuseException {
    // Get the subjects code
    Element element = xnatfs.sContentCache.get ( mPath );
    HashSet<String> subjectList = null;
    if ( element == null ) {
      try {
        subjectList = new HashSet<String>();
        InputStreamReader reader = new InputStreamReader ( XNATConnection.getInstance().getURLAsStream ( mPath + "?format=json" ) );
        JSONTokener tokenizer = new JSONTokener ( reader );
        JSONObject json = new JSONObject ( tokenizer );
        JSONArray subjects = json.getJSONObject ( "ResultSet" ).getJSONArray ( "Result" );
        for ( int idx = 0; idx < subjects.length(); idx++ ) {
          if ( subjects.isNull ( idx ) ) { continue; }
          String id = subjects.getJSONObject ( idx ).getString ( "subjectid" );
          subjectList.add ( id );
        }
      } catch ( Exception e ) {
        logger.error ( "Caught exception reading " + mPath, e );
        throw new FuseException();
      }
      element = new Element ( mPath, subjectList );
      xnatfs.sContentCache.put ( element );
    }
    subjectList = (HashSet<String>) element.getObjectValue();
    return subjectList;
  }

  /** Create a child of this node.  Note, the child is a single filename, not a path
   */
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath  );
    HashSet<String> subjectList = getSubjectList();
    if ( subjectList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) { return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue() ); }
      Element element = new Element ( childPath, new Subject ( childPath, child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node)element.getObjectValue();
    }
    return null;
  }
         
}
