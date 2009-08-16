package org.xnat.xnatfs;

import fuse.*;

import java.util.*;
import org.apache.log4j.*;
import net.sf.ehcache.*;

/**
 * Class to handle Projects. Implemented as a directory with a sub-directory for
 * each project, and a RemoteListFile for the project from the REST API.
 * 
 * @author blezek
 * 
 */
public class Projects extends Container {

  private static final Logger logger = Logger.getLogger ( Projects.class );

  /**
   * Constructor for a Projects, requires a path which maps into the REST api.
   * 
   * @param path
   *          Path for the Projects directory.
   */
  public Projects ( String path ) {
    super ( path );
    mChildKey = "id";
  }

  /**
   * Indicates this Node is a directory.
   * 
   * @see org.xnat.xnatfs.Node#getattr(java.lang.String, fuse.FuseGetattrSetter)
   */
  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      // set(long inode, int mode, int nlink, int uid, int gid, int rdev, long
      // size, long blocks, int atime, int mtime, int ctime)
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  /**
   * Create a child of this node. Note, the child is a single filename, not a
   * path. Children are found in by the Container class based on the mChildKey.
   * 
   * @param child
   *          Name of the child to create.
   */
  @Override
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath );
    HashSet<String> projectList = getElementList ();
    if ( projectList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) (xnatfs.sNodeCache.get ( childPath ).getObjectValue ());
      }
      Element element = new Element ( childPath, new Project ( childPath, child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return super.createChild ( child );
  }

}
