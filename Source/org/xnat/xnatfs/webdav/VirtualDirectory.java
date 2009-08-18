package org.xnat.xnatfs.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

abstract public class VirtualDirectory extends VirtualResource implements CollectionResource, GetableResource {
  String mAbsolutePath;
  String mChildKey;
  static final Logger logger = Logger.getLogger ( VirtualResource.class );

  public VirtualDirectory ( XNATFS x, String path, String name ) {
    super ( x, path, name );
    if ( path.equals ( "/" ) && mName.equals ( "/" ) ) {
      mAbsolutePath = "/";
    } else {
      mAbsolutePath = path + "/" + name;
    }
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

}
