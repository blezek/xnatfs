package org.xnat.xnatfs.test;

import fuse.*;

import org.junit.*;
import static org.junit.Assert.*;
import org.xnat.xnatfs.*;
import org.apache.log4j.*;

public class RemoteFileHandleTest {

  static xnatfs x = new xnatfs ();

  @BeforeClass
  public static void configure () {
    BasicConfigurator.configure ();
    xnatfs.configureCache ();
    xnatfs.configureConnection ();
  }

  @Ignore
  public void testLargeFile () throws Exception {
    try {
      RemoteFileHandle files = new RemoteFileHandle ( "http://central.xnat.org/REST/projects/CENTRAL_OASIS_CS/subjects/OAS1_0456/experiments/OAS1_0456_MR1/scans/mpr-1/files", "files" );
      assertTrue ( files.getBytes () != null );
    } catch ( Exception e1 ) {
      fail ( "Failed to get remote file: " + e1 );
    }

    try {
      RemoteFileHandle bigFile = new RemoteFileHandle (
          "http://central.xnat.org/REST/projects/CENTRAL_OASIS_CS/subjects/OAS1_0456/experiments/OAS1_0456_MR1/scans/mpr-1/resources/308/files/OAS1_0456_MR1_mpr-1_anon.img", "bigfile" );
      assertTrue ( bigFile.getBytes () != null );
    } catch ( Exception e2 ) {
      fail ( "Failed to get large remote file: " + e2 );
    }
  }

  @Test
  public void testOpen () throws Exception {
    String path = "/projects/CENTRAL_OASIS_CS/subjects/OAS1_0457/experiments/OAS1_0457_MR1/scans/mpr-1/files/OAS1_0457_MR1_mpr-1_anon.img";
    FuseOpenSetter openSetter = new FuseOpen ();
    x.open ( path, 0, openSetter );
  }
}
