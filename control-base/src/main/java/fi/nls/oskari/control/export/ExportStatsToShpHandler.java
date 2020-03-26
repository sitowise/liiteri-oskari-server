package fi.nls.oskari.control.export;

import java.io.*;
import javax.servlet.http.HttpServletResponse;

import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.service.ShapefileService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

@OskariActionRoute("ExportStatsToShpHandler")
public class ExportStatsToShpHandler extends ActionHandler {
	private static final String PARAM_FEATURE_COLLECTION = "featureCollection";
	private static final String PARAM_FILE_NAME = "fileName";
	private static final String CONTENT_TYPE_ZIP = "application/zip";
	private static final Logger LOGGER = LogFactory.getLogger(ExportStatsToShpHandler.class);
	private static final String DEFAULT_OUTPUT_FILE_NAME = "statistics";
	private ShapefileService service;
	
	@Override
	public void init() {
		service = new ShapefileService();
	}
	
	@Override
	public void handleAction(final ActionParameters params) throws ActionException {
		
		User user = params.getUser();
		if (user.isGuest()) {
			throw new ActionDeniedException("User is not logged");
		}
		
		String featureCollectionParam = params.getRequiredParam(PARAM_FEATURE_COLLECTION);
		String fileName = params.getHttpParam(PARAM_FILE_NAME, DEFAULT_OUTPUT_FILE_NAME);
		
		try {
			final HttpServletResponse response = params.getResponse();
			response.setContentType(CONTENT_TYPE_ZIP);
			response.setHeader("Content-disposition", "attachment; filename=" + fileName + ".zip");
			OutputStream out;
			try {
				out = response.getOutputStream();
			} catch (IOException ioe) {
				throw new ActionException(ioe.getMessage(), ioe);
			}
			
			service.exportStatisticsToShp(out, featureCollectionParam, fileName);
			
		} catch (Exception e) {
			LOGGER.error("Could not handle ExportStatsToShpHandler request");
			throw new ActionException("Could not handle ExportStatsToShpHandler request: ", e);
		}
	}
}