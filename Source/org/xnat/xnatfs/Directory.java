package org.xnat.xnatfs;

import fuse.compat.*;
import fuse.*;

import java.util.*;

/**
 * A directory class
 */
public class Directory extends Node {
  // private Map<String, Node> mChildren;

  public Directory ( String path ) {
    super ( path );
    // mChildren = new HashMap<String, Node> ();
  }

  public void getdir ( FuseDirFiller filler ) {
  }

  protected FuseStat createStat () {
    FuseStat stat = new FuseStat ();
    stat.mode = FuseFtype.TYPE_DIR | 0755;
    stat.uid = stat.gid = 0;
    stat.ctime = stat.mtime = stat.atime = (int) (System.currentTimeMillis () / 1000L);
    stat.size = 0;
    stat.blocks = 0;
    return stat;
  }

  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    // TODO Auto-generated method stub
    return 0;
  }

}
