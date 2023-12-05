package scc.srv;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import scc.db.MongoDBLayer;

public class AppServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Initialization logic when the servlet context is initialized
        MongoDBLayer.getInstance().initializeCollections();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup logic when the servlet context is destroyed
    }
}