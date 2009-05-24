
/** Handle file requests
 */

package org.xnat.xnatfs;

import fuse.FuseDirFiller;
import fuse.FuseFtypeConstants;
import fuse.FuseGetattrSetter;

public class FileHandler {

  public void getdir ( String path, String name, FuseDirFiller filler ) {
    String n;
    n = name + ".xml";
    filler.add ( n, n.hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
    n = name + ".csv";
    filler.add ( n, n.hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
    n = name + ".json";
    filler.add ( n, n.hashCode (), FuseFtypeConstants.TYPE_DIR | 0555 );
  }

  public int count ( String path, String name ) {
    return 3;
  }

  public int getattr ( String path, String name, FuseGetattrSetter setter ) {
    int time = (int) (System.currentTimeMillis () / 1000L);
    String n;
    n = name + ".xml";
    // Get and cache the file
    setter.set ( n.hashCode (), FuseFtypeConstants.TYPE_FILE | 0644, 1, 0, 0, 0, 10, (10 + xnatfs.BLOCK_SIZE - 1) / xnatfs.BLOCK_SIZE, time, time, time );

    return 0;

  }
}
