package org.xnat.xnatfs.webdav;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.GetHandler;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class GetHandlerLocal extends GetHandler {
  public GetHandlerLocal ( HttpManager manager ) {
    super ( manager );
  }

  static Logger logger = Logger.getLogger ( GetHandlerLocal.class );

  @Override
  public void process ( HttpManager manager, Request request, Response response ) throws NotAuthorizedException, ConflictException {
    String host = request.getHostHeader ();
    String url;
    url = request.getAbsoluteUrl ();

    logger.debug ( "find resource: " + url );
    Resource r = manager.getResourceFactory ().getResource ( host, url );
    if ( r != null ) {
      processResource ( manager, request, response, r );
    } else {
      respondNotFound ( response, request );
    }
  }

}
