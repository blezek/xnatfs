package org.xnat.xnatfs;

import java.io.InputStreamReader;
import java.util.HashSet;

import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import fuse.FuseException;

public abstract class Container extends Node {
	  private static final Logger logger = Logger.getLogger(Container.class);

	public Container(String path) {
		super(path);
	}

	protected HashSet<String> getElementList(String field) throws FuseException {
	    // Get the subjects code
	    Element element = xnatfs.sContentCache.get ( mPath );
	    HashSet<String> list = null;
	    if ( element == null ) {
	      try {
	        list = new HashSet<String>();
	        InputStreamReader reader = new InputStreamReader ( XNATConnection.getInstance().getURLAsStream ( mPath + "?format=json" ) );
	        JSONTokener tokenizer = new JSONTokener ( reader );
	        JSONObject json = new JSONObject ( tokenizer );
	        JSONArray subjects = json.getJSONObject ( "ResultSet" ).getJSONArray ( "Result" );
	        logger.debug( "Found: " + subjects.length() + " elements" );
	        for ( int idx = 0; idx < subjects.length(); idx++ ) {
	          if ( subjects.isNull ( idx ) ) { continue; }
	          String id = subjects.getJSONObject ( idx ).getString ( field );
	          list.add ( id );
	        }
	      } catch ( Exception e ) {
	        logger.error ( "Caught exception reading " + mPath, e );
	        throw new FuseException();
	      }
	      element = new Element ( mPath, list );
	      xnatfs.sContentCache.put ( element );
	    }
	    list = (HashSet<String>) element.getObjectValue();
	    return list;
	  }

}