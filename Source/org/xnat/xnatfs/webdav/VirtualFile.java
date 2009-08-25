package org.xnat.xnatfs.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;

abstract public class VirtualFile extends VirtualResource implements GetableResource {
  public VirtualFile ( XNATFS x, String path, String name ) {
    super ( x, path, name );
  }

  public String getContentType ( String accepts ) {
    // TODO Auto-generated method stub
    return null;
  }

  public Long getMaxAgeSeconds ( Auth auth ) {
    // TODO Auto-generated method stub
    return new Long ( 1200 );
  }

}
