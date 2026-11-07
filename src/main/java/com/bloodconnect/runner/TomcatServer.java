package com.bloodconnect.runner;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Embedded Apache Tomcat 10.1 server launcher for local execution, development,
 * and academic evaluation without requiring a separate external Tomcat installation.
 */
public class TomcatServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String contextPath = "/BloodConnect";

        // Read configuration
        try (InputStream in = TomcatServer.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                port = Integer.parseInt(props.getProperty("app.port", "8080"));
                contextPath = props.getProperty("app.context", "/BloodConnect");
            }
        } catch (Exception ignored) {
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir("target/tomcat");
        tomcat.getConnector(); // Initialize default HTTP connector

        // Webapp docbase
        String docBase = new File("src/main/webapp").getAbsolutePath();
        if (!new File(docBase).exists()) {
            docBase = new File("webapp").getAbsolutePath();
        }

        System.out.println("========================================================");
        System.out.println("Starting Apache Tomcat 10.1 for BloodConnect");
        System.out.println("DocBase: " + docBase);
        System.out.println("Port: " + port);
        System.out.println("Context Path: " + contextPath);
        System.out.println("Local URL: http://localhost:" + port + contextPath + "/");
        System.out.println("========================================================");

        // Mount the primary application at /BloodConnect
        Context ctx = tomcat.addWebapp(contextPath, docBase);
        ctx.setParentClassLoader(TomcatServer.class.getClassLoader());

        // Ensure target/classes are visible to webapp resource root in IDE / exec:java
        File additionWebInfClasses = new File("target/classes");
        if (additionWebInfClasses.exists()) {
            WebResourceRoot resources = new StandardRoot(ctx);
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                    additionWebInfClasses.getAbsolutePath(), "/"));
            ctx.setResources(resources);
        }

        // Mount a small root redirect at / -> /BloodConnect/
        File emptyDir = Files.createTempDirectory("tomcat-root").toFile();
        emptyDir.deleteOnExit();
        Context rootCtx = tomcat.addContext("", emptyDir.getAbsolutePath());
        rootCtx.setParentClassLoader(TomcatServer.class.getClassLoader());

        Tomcat.addServlet(rootCtx, "RootRedirectServlet", new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void doGet(jakarta.servlet.http.HttpServletRequest req,
                                jakarta.servlet.http.HttpServletResponse resp) throws java.io.IOException {
                resp.sendRedirect("/BloodConnect/");
            }
        });
        rootCtx.addServletMappingDecoded("", "RootRedirectServlet");
        rootCtx.addServletMappingDecoded("/", "RootRedirectServlet");

        tomcat.start();
        System.out.println("Tomcat 10.1 is running successfully at http://localhost:" + port + contextPath + "/");
        tomcat.getServer().await();
    }
}
