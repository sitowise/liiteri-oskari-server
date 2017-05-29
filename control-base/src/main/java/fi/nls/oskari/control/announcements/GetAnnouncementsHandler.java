package fi.nls.oskari.control.announcements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.announcements.service.AnnouncementsDbService;
import fi.nls.oskari.announcements.service.AnnouncementsDbServiceIbatisImpl;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.announcements.Announcement;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetAnnouncements")
public class GetAnnouncementsHandler extends ActionHandler {

	private static final Logger log = LogFactory
			.getLogger(GetAnnouncementsHandler.class);
	private static final AnnouncementsDbService announcementsService = new AnnouncementsDbServiceIbatisImpl();

	@Override
	public void handleAction(ActionParameters params) throws ActionException {
		List<Announcement> announcements;
		Date expirationDate;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			expirationDate = sdf.parse(sdf.format(new Date()));

		} catch (ParseException e1) {
			throw new ActionException("Error during date parsing");
		}

		try {
			announcements = announcementsService
					.getAnnouncements(expirationDate);
		} catch (Exception e) {
			throw new ActionException(
					"Error during selecting required data from database");
		}

		try {
			JSONObject main = JSONAnnouncementHelper
					.createAnnouncementsJSONOutput(announcements);
			ResponseHelper.writeResponse(params, main);
		} catch (Exception e) {
			throw new ActionException(
					"Error during creating JSON announcements object");
			}
	}

}
