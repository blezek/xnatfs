/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import net.sf.ehcache.Element;

import org.apache.http.HttpEntity;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.bradmcevoy.http.Resource;

/**
 * @author blezek
 * 
 */
public class Bundle extends VirtualDirectory {
  final static Logger logger = Logger.getLogger ( Bundle.class );

  /**
   * @param x
   * @param path
   * @param name
   * @param url
   */
  public Bundle ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name, url );
    mElementURL = mURL + "files?format=json";
    mChildKey = "Name";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.webdav.VirtualDirectory#child(java.lang.String)
   */
  @Override
  public Resource child ( String childName ) {
    logger.debug ( "child: create " + childName );
    String childPath = mAbsolutePath + childName;
    HashMap<String, ArrayList<String>> s = null;
    try {
      ArrayList<String> l = new ArrayList<String> ();
      l.add ( "URI" );
      l.add ( "Size" );
      s = getFileMap ( mElementURL, "Name", l );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( s.containsKey ( childName ) ) {
      if ( XNATFS.sNodeCache.get ( childPath ) != null ) {
        return (Resource) ( XNATFS.sNodeCache.get ( childPath ).getObjectValue () );
      }

      long size = Long.valueOf ( s.get ( childName ).get ( 1 ) ).longValue ();
      RemoteFile remote = new RemoteFile ( xnatfs, mAbsolutePath, childName, mURL + "files/" + childName, size );
      Element element = new Element ( childPath, remote );
      XNATFS.sNodeCache.put ( element );
      return (Resource) element.getObjectValue ();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  protected synchronized HashMap<String, ArrayList<String>> getFileMap ( String url, String inKey, ArrayList<String> valueFields ) throws IOException {
    // Get the subjects code
    Element element = XNATFS.sContentCache.get ( "FileMap::" + url );
    HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>> ();
    if ( element == null ) {
      HttpEntity entity = null;
      try {
        entity = Connection.getInstance ().getEntity ( url );
        InputStreamReader reader = new InputStreamReader ( entity.getContent () );
        JSONTokener tokenizer = new JSONTokener ( reader );
        JSONObject json = new JSONObject ( tokenizer );
        JSONArray subjects = json.getJSONObject ( "ResultSet" ).getJSONArray ( "Result" );
        logger.debug ( "Found: " + subjects.length () + " elements" );
        for ( int idx = 0; idx < subjects.length (); idx++ ) {
          if ( subjects.isNull ( idx ) ) {
            continue;
          }
          String key = subjects.getJSONObject ( idx ).getString ( inKey );
          ArrayList<String> values = new ArrayList<String> ();
          for ( String value : valueFields ) {
            values.add ( subjects.getJSONObject ( idx ).getString ( value ) );
          }
          map.put ( key, values );
        }
      } catch ( Exception e ) {
        logger.error ( "Caught exception reading " + url, e );
        throw new IOException ( "Caught exception reading " + url );
      } finally {
        if ( entity != null ) {
          entity.consumeContent ();
        }
      }
      element = new Element ( "FileMap::" + url, map );
      XNATFS.sContentCache.put ( element );
    }
    map = (HashMap<String, ArrayList<String>>) element.getObjectValue ();
    return map;
  }
}
