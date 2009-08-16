/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnat.xnatfs.Start;

import com.bradmcevoy.http.AbstractMiltonEndPoint;
import com.bradmcevoy.http.ApplicationConfig;
import com.bradmcevoy.http.MiltonServlet;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.ServletRequest;
import com.bradmcevoy.http.ServletResponse;

/**
 * @author blezek
 * 
 */
public class xnatfsServlet extends AbstractMiltonEndPoint implements Servlet {

  private Logger log = LoggerFactory.getLogger ( MiltonServlet.class );

  private ServletConfig config;

  private static final ThreadLocal<HttpServletRequest> originalRequest = new ThreadLocal<HttpServletRequest> ();
  private static final ThreadLocal<HttpServletResponse> originalResponse = new ThreadLocal<HttpServletResponse> ();

  public void init ( ServletConfig config ) throws ServletException {

    try {
      this.config = config;
      String notFoundPath = config.getInitParameter ( "not.found.path" );
      String resourceFactoryFactoryClassName = config.getInitParameter ( "resource.factory.factory.class" );
      if ( resourceFactoryFactoryClassName != null && resourceFactoryFactoryClassName.length () > 0 ) {
        initFromFactoryFactory ( resourceFactoryFactoryClassName, notFoundPath );
      } else {
        String resourceFactoryClassName = config.getInitParameter ( "resource.factory.class" );
        String responseHandlerClassName = config.getInitParameter ( "response.handler.class" );
        init ( resourceFactoryClassName, notFoundPath, responseHandlerClassName );
      }
      httpManager.init ( new ApplicationConfig ( config ), httpManager );
    } catch ( ServletException ex ) {
      log.error ( "Exception starting milton servlet", ex );
      throw ex;
    } catch ( Throwable ex ) {
      log.error ( "Exception starting milton servlet", ex );
      throw new RuntimeException ( ex );
    }
  }

  public static HttpServletRequest request () {
    return originalRequest.get ();
  }

  public static HttpServletResponse response () {
    return originalResponse.get ();
  }

  public static void forward ( String url ) {
    try {
      request ().getRequestDispatcher ( url ).forward ( originalRequest.get (), originalResponse.get () );
    } catch ( IOException ex ) {
      throw new RuntimeException ( ex );
    } catch ( ServletException ex ) {
      throw new RuntimeException ( ex );
    }
  }

  public void service ( javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse ) throws ServletException, IOException {
    HttpServletRequest req = (HttpServletRequest) servletRequest;
    HttpServletResponse resp = (HttpServletResponse) servletResponse;
    try {
      originalRequest.set ( req );
      originalResponse.set ( resp );
      Request request = new ServletRequest ( req );
      Response response = new ServletResponse ( resp );
      httpManager.process ( request, response );
    } finally {
      originalRequest.remove ();
      originalResponse.remove ();
      servletResponse.getOutputStream ().flush ();
      servletResponse.flushBuffer ();
    }
  }

  public String getServletInfo () {
    return "MiltonServlet";
  }

  public ServletConfig getServletConfig () {
    return config;
  }
}
