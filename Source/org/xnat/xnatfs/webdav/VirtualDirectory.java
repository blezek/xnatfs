package org.xnat.xnatfs.webdav;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Element;

import org.apache.http.HttpEntity;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import edu.emory.mathcs.backport.java.util.Collections;
import fuse.FuseException;

abstract public class VirtualDirectory extends VirtualResource implements CollectionResource, GetableResource {
  String mChildKey;
  static final Logger logger = Logger.getLogger ( VirtualDirectory.class );
  String mURL;
  String mElementURL;

  public VirtualDirectory ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name );
    mURL = url;
    mChildKey = "id";
    mElementURL = null;
  }

  abstract public Resource child ( String arg0 );

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.webdav.VirtualDirectory#getChildren()
   */
  public List<? extends Resource> getChildren () {
    HashSet<String> s = null;
    try {
      s = getElementList ( mElementURL, mChildKey );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    ArrayList<Resource> list = new ArrayList<Resource> ();
    for ( String child : s ) {
      logger.debug ( "got Child " + child );
      list.add ( child ( child ) );
    }
    Collections.sort ( list, new Comparator<Resource> () {
      public int compare ( Resource o1, Resource o2 ) {
        return o1.getName ().compareTo ( o2.getName () );
      }
    } );
    return list;
  }

  public void sendContent ( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException {
    XmlWriter w = new XmlWriter ( out );
    w.open ( "html" );
    w.open ( "body" );
    w.begin ( "h1" ).open ().writeText ( this.getName () ).close ();
    w.open ( "table" );
    w.open ( "tr" );
    w.open ( "td" );
    w.begin ( "a" ).writeAtt ( "href", ".." ).open ().writeText ( "ParentDirectory" ).close ();
    w.close ( "td" );
    w.close ( "td" );
    for ( Resource rr : getChildren () ) {
      if ( rr == null || !( rr instanceof VirtualResource ) ) {
        logger.error ( "Child is null or not a VirtualResource!" );
      } else {
        VirtualResource r = (VirtualResource) rr;
        w.open ( "tr" );

        w.open ( "td" );
        String href = r.getName ();
        if ( r instanceof VirtualDirectory ) {
          href = href + "/";
        }
        w.begin ( "a" ).writeAtt ( "href", href ).open ().writeText ( r.getName () ).close ();
        // logger.debug ( "sendContent: added reference " + r.getName () +
        // " to href " + href );
        w.close ( "td" );

        w.begin ( "td" ).open ().writeText ( r.getModifiedDate () + "" ).close ();
        w.close ( "tr" );
      }
    }
    w.close ( "table" );
    w.close ( "body" );
    w.close ( "html" );
    w.flush ();
  }

  public String getContentType ( String accepts ) {
    return "text/html";
  }

  public Long getContentLength () {
    return null;
  }

  public Long getMaxAgeSeconds ( Auth auth ) {
    return null;
  }

  /**
   * Get sub-elements of this element. Return a set of names. Used to populate
   * the directory, and calls <code>createChild</code>, delegating to
   * subclasses.
   * 
   * @return Set of strings of the contained elements.
   * @throws FuseException
   */
  protected HashSet<String> getElementList () throws Exception {
    return getElementList ( mPath, null );
  }

  protected HashSet<String> getElementList ( String inKey ) throws Exception {
    return getElementList ( mPath, inKey );
  }

  @SuppressWarnings("unchecked")
  protected HashSet<String> getElementList ( String url, String inKey ) throws Exception {
    // See if we cached the list already
    Element e = XNATFS.sContentCache.get ( "ElementList::" + url );
    if ( e != null ) {
      return (HashSet<String>) e.getObjectValue ();
    }
    // Get the subjects code
    HashSet<String> list = new HashSet<String> ();
    HttpEntity entity = null;
    try {
      entity = Connection.getInstance ().getEntity ( url );
      InputStreamReader reader = new InputStreamReader ( entity.getContent () );
      JSONTokener tokenizer = new JSONTokener ( reader );
      JSONObject json = new JSONObject ( tokenizer );
      JSONArray subjects = json.getJSONObject ( "ResultSet" ).getJSONArray ( "Result" );
      // logger.debug ( "Found: " + subjects.length () + " elements" );
      for ( int idx = 0; idx < subjects.length (); idx++ ) {
        if ( subjects.isNull ( idx ) ) {
          continue;
        }

        String id = null;
        if ( inKey != null ) {
          id = subjects.getJSONObject ( idx ).getString ( inKey );
          // logger.debug ( "Found " + id + " from " + inKey );
        }
        if ( id == null ) {
          id = subjects.getJSONObject ( idx ).getString ( mChildKey );
        }
        list.add ( id );
      }
    } catch ( Exception e1 ) {
      logger.error ( "Caught exception reading " + url, e1 );
      HttpEntity e2 = Connection.getInstance ().getEntity ( url );
      InputStreamReader reader = new InputStreamReader ( e2.getContent () );
      char buffer[] = new char[1024];
      int readcount = reader.read ( buffer );
      while ( readcount != -1 ) {
        logger.error ( new String ( buffer, 0, readcount ) );
        readcount = reader.read ( buffer );
      }
      e2.consumeContent ();
      throw e1;
    } finally {
      if ( entity != null ) {
        entity.consumeContent ();
      }
    }
    // Cache it
    XNATFS.sContentCache.put ( new Element ( "ElementList::" + url, list ) );
    return list;
  }
}
