package org.xnat.xnatfs.webdav;

import com.bradmcevoy.http.GetableResource;

abstract public class VirtualFile extends VirtualResource implements GetableResource {

    public VirtualFile ( XNATFS x, String path, String name ) {
        super ( x, path, name);
 
    }
}
