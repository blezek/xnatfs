package org.xnat.xnatfs.webdav;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

import fuse.FuseException;

abstract public class VirtualDirectory extends VirtualResource implements CollectionResource, GetableResource {
  String mChildKey;
  static final Logger logger = Logger.getLogger ( VirtualResource.class );

  public VirtualDirectory ( XNATFS x, String path, String name ) {
    super ( x, path, name );
    mChildKey = "id";
  }

  abstract public Resource child ( String arg0 );

  abstract public List<? extends Resource> getChildren ();

  public void sendContent ( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException {
    XmlWriter w = new XmlWriter ( out );
    w.open ( "html" );
    w.open ( "body" );
    w.begin ( "h1" ).open ().writeText ( this.getName () ).close ();
    w.open ( "table" );
    for ( Resource r : getChildren () ) {
      if ( r == null ) {
        logger.error ( "Child is null!" );
      } else {
        w.open ( "tr" );

        w.open ( "td" );
        w.begin ( "a" ).writeAtt ( "href", r.getName () ).open ().writeText ( r.getName () ).close ();
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
      logger.debug ( "Found: " + subjects.length () + " elements" );
      for ( int idx = 0; idx < subjects.length (); idx++ ) {
        if ( subjects.isNull ( idx ) ) {
          continue;
        }

        String id = null;
        if ( inKey != null ) {
          id = subjects.getJSONObject ( idx ).getString ( inKey );
          logger.debug ( "Found " + id + " from " + inKey );
        }
        if ( id == null ) {
          id = subjects.getJSONObject ( idx ).getString ( mChildKey );
        }
        list.add ( id );
      }
    } catch ( Exception e1 ) {
      logger.error ( "Caught exception reading " + url, e1 );
      throw new FuseException ();
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
