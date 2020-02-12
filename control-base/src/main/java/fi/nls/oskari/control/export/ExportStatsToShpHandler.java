package fi.nls.oskari.control.export;

import java.io.*;
import javax.servlet.http.HttpServletResponse;

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
	private static final String CONTENT_TYPE_ZIP = "application/zip";
	private static final Logger logger = LogFactory.getLogger(ExportStatsToShpHandler.class);
	private static final String OUTPUT_FILE_NAME = "statistics";
	private ShapefileService service;
	
	@Override
	public void init() {
		service = new ShapefileService(OUTPUT_FILE_NAME);
	}
	
	@Override
	public void handleAction(final ActionParameters params) throws ActionException {
		
		String featureCollectionParam = params.getRequiredParam(PARAM_FEATURE_COLLECTION);
		
		try {
			final HttpServletResponse response = params.getResponse();
			response.setContentType(CONTENT_TYPE_ZIP);
			response.setHeader("Content-disposition", "attachment; filename=" + OUTPUT_FILE_NAME + ".zip");
			OutputStream out;
			try {
				out = response.getOutputStream();
			} catch (IOException ioe) {
				throw new ActionException(ioe.getMessage(), ioe);
			}
			
			service.exportStatisticsToShp(out, featureCollectionParam);
			
		} catch (Exception e) {
			throw new ActionException("Could not handle ExportStatsToShpHandler request: ", e);
		}
	}
}