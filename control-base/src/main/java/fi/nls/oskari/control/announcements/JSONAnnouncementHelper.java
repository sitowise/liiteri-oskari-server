package fi.nls.oskari.control.announcements;

import java.text.SimpleDateFormat;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.nls.oskari.domain.announcements.Announcement;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;

public class JSONAnnouncementHelper {

	private static final Logger log = LogFactory
			.getLogger(JSONAnnouncementHelper.class);

	public static final JSONObject createAnnouncementsJSONOutput(
			List<Announcement> announcements) {
		JSONArray outputArray = new JSONArray();

		for (Announcement a : announcements) {
			JSONObject ob = new JSONObject();
			JSONHelper.putValue(ob, "id", a.getId());
			JSONHelper.putValue(ob, "title", a.getTitle());
			JSONHelper.putValue(ob, "message", a.getMessage());
			JSONHelper.putValue(ob, "expirationDate", new SimpleDateFormat(
					"yyyy-MM-dd").format(a.getExpirationDate()));

			outputArray.put(ob);
		}

		JSONObject outputObject = new JSONObject();
		JSONHelper.putValue(outputObject, "announcements", outputArray);

		return outputObject;
	}
}
