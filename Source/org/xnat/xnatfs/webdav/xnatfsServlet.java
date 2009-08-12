/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.File;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.xnat.xnatfs.Start;

import com.bradmcevoy.http.MiltonServlet;

/**
 * @author blezek
 * 
 */
public class xnatfsServlet extends MiltonServlet {

  public void init ( ServletConfig config ) throws ServletException {
    // Configure log4j
    // First try to configure from local directory, then from application
    // support directory
    File props;
    props = new File ( System.getProperty ( "user.dir", "." ), "log4j.properties" );
    if ( props.exists () && props.canRead () ) {
      PropertyConfigurator.configure ( props.getAbsolutePath () );
    } else {
      props = new File ( Start.getApplicationResourceDirectory ( "xnatfs" ), "log4j.properties" );
      if ( props.exists () && props.canRead () ) {
        PropertyConfigurator.configure ( props.getAbsolutePath () );
      } else {
        URL url = xnatfsServlet.class.getResource ( "/log4j.properties" );
        if ( url != null ) {
          PropertyConfigurator.configure ( url );
        } else {
          BasicConfigurator.configure ();
        }
      }
    }

    super.init ( config );
  }
}
