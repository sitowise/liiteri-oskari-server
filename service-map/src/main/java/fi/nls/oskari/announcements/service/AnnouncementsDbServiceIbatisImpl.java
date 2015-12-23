package fi.nls.oskari.announcements.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.ibatis.sqlmap.client.SqlMapSession;
import fi.nls.oskari.domain.announcements.Announcement;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class AnnouncementsDbServiceIbatisImpl extends
		BaseIbatisService<Announcement> implements AnnouncementsDbService {

	@Override
	protected String getNameSpace() {
		return "Announcements";
	}

	@Override
	public List<Announcement> getAnnouncements(Date expirationDate)
			throws ServiceException {

		try {
			List<Announcement> announcements = queryForList(getNameSpace()
					+ ".getAllAnnouncements");

			List<Announcement> announcementsToReturn = new ArrayList<Announcement>();
			for (Announcement a : announcements) {
				if (expirationDate.before(a.getExpirationDate())) {
					announcementsToReturn.add(a);
				}
			}
			return announcementsToReturn;

		} catch (Exception e) {
			throw new ServiceException("Error getting list of announcements", e);
		}
	}

	@Override
	public long insertAnnouncement(Announcement announcement)
			throws ServiceException {
		final SqlMapSession session = openSession();
		final Long id;
		try {
			session.startTransaction();

			id = (Long) session.queryForObject(getNameSpace()
					+ ".insertAnnouncement", announcement);

			session.commitTransaction();
		} catch (Exception e) {
			throw new ServiceException("Error inserting announcement:"
					+ announcement.getId(), e);
		} finally {
			endSession(session);
		}
		return id;
	}

	@Override
	public long updateAnnouncement(Announcement announcement)
			throws ServiceException {
		final SqlMapSession session = openSession();
		final Long id;
		try {
			session.startTransaction();

			id = (long) session.update(getNameSpace()
					+ ".updateAnnouncement", announcement);

			session.commitTransaction();
		} catch (Exception e) {
			throw new ServiceException("Error updating announcement:"
					+ announcement.getId(), e);
		} finally {
			endSession(session);
		}
		return id;
	}

	@Override
	public long deleteAnnouncement(long id) throws ServiceException {
		final SqlMapSession session = openSession();
		final Long rowCount;
		try {
			session.startTransaction();

			rowCount = (long) session.delete(getNameSpace()
					+ ".deleteAnnouncement", id);

			session.commitTransaction();
		} catch (Exception e) {
			throw new ServiceException("Error deleting announcement:" + id, e);
		} finally {
			endSession(session);
		}
		return rowCount;
	}
}
