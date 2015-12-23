package fi.nls.oskari.announcements.service;

import java.util.Date;
import java.util.List;

import fi.nls.oskari.domain.announcements.Announcement;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface AnnouncementsDbService extends BaseService<Announcement> {

	public List<Announcement> getAnnouncements(Date expirationDate)
			throws ServiceException;

	public long insertAnnouncement(final Announcement announcement)
			throws ServiceException;

	public long updateAnnouncement(final Announcement announcement)
			throws ServiceException;

	public long deleteAnnouncement(long id) throws ServiceException;

}
