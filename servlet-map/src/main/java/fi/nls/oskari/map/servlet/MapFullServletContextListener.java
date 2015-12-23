package fi.nls.oskari.map.servlet;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import fi.nls.oskari.control.data.GisDataReminder;
import fi.nls.oskari.control.workspaces.WorkspaceReminder;

public class MapFullServletContextListener implements ServletContextListener {

	private ScheduledExecutorService scheduler;
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(new WorkspaceReminder(), 0, 1, TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(new GisDataReminder(), 0, 1, TimeUnit.MINUTES);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		scheduler.shutdownNow();
		
	}

}
