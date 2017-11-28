package fi.nls.oskari.groupings.db;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.ibatis.sqlmap.client.SqlMapSession;

import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.domain.groupings.GroupingThemeData;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

public class GroupingServiceIbatisImpl extends BaseIbatisService<Grouping>
		implements GroupingDbService {

	private static final String SERVER_NAME = "groupings.sharing.serverName";
	private static final String TITLE = "groupings.sharing.email.title";
	private static final String CONTENT_TEXT = "groupings.sharing.email.contentText";
	private static final String SMTP_HOST_SERVER = "mailing.smtpHostServer";
	private static final String SENDER_ADDRESS = "mailing.senderAddress";
	private static final String SENDER_NAME = "mailing.senderName";

	@Override
	protected String getNameSpace() {
		return "Groupings";
	}
	
	@Override
	public List<Grouping> findByIds(List<Long> ids) throws ServiceException
	{
		return queryForList(getNameSpace() + ".findByIds",  ids);
	}

	public long insertGrouping(final Grouping grouping) throws ServiceException {
		final Long id;
		if (grouping == null) {
			throw new ServiceException(
					"Tried to insert groupings with <null> param");
		}
		
		final SqlMapSession session = openSession();
		try {
			session.startTransaction();
			Date d = new Date();
			grouping.setCreated(d);
			grouping.setUpdated(d);

			id = (Long) session.queryForObject(getNameSpace()
					+ ".insertGrouping", grouping);
			for (GroupingTheme t : grouping.getThemes()) {
				insertThemes(session, null, t, id, null);
			}

			session.commitTransaction();
		} catch (Exception e) {
			throw new ServiceException("Error inserting grouping:"
					+ grouping.getName(), e);
		} finally {
			endSession(session);
		}
		return id;
	}

	private long insertThemes(SqlMapSession session, Long parentId,
			GroupingTheme theme, Long oskariGroupingId, Long mainThemeId)
			throws SQLException {
		Long masterThemeId = null;
		theme.setParentThemeId(parentId);
		theme.setOskariGroupingId(oskariGroupingId);
		theme.setMainThemeId(mainThemeId);
		final Long id = (Long) session.queryForObject(getNameSpace()
				+ ".insertGroupingTheme", theme);
		masterThemeId = mainThemeId == null ? id : mainThemeId;
		for (GroupingTheme t : theme.getSubThemes()) {
			insertThemes(session, id, t, oskariGroupingId, masterThemeId);
		}
		if (theme.getThemeData().size() > 0) {
			for (GroupingThemeData d : theme.getThemeData()) {
				d.setOskariGroupingThemeId(id);
				session.insert(getNameSpace() + ".insertGroupingThemeData", d);
			}
		}

		return id;
	}

	/**
	 * Checking if the theme has any content. 
	 * NOTE! Now it's unused because of new requirements.
	 * @param theme
	 * @throws ServiceException when the theme has no elements
	 */
	private void CheckGroupingData(GroupingTheme theme) throws ServiceException {
		if (theme.getSubThemes().size() > 0 && theme.getThemeData().size() == 0) {
			for (GroupingTheme t : theme.getSubThemes()) {
				CheckGroupingData(t);
			}
		} else if (theme.getSubThemes().size() == 0
				&& theme.getThemeData().size() == 0) {
			throw new ServiceException(
					"There are themes without elements binded");
		}
	}

	@Override
	public int deleteGrouping(long id) throws ServiceException {
		int rowCount = 0;
		final SqlMapSession session = openSession();
		try {
			rowCount = (int) session.delete(getNameSpace() + ".deleteGrouping",
					id);
		} catch (Exception e) {
			throw new ServiceException("Error deleting grouping:" + id, e);
		} finally {
			try {
				// MUST be closed if explicitly opened (via openSession()).
				session.close();
			} catch (Exception ignored) {
			}
		}
		return rowCount;
	}

	@Override
	public int updateGrouping(Grouping grouping) throws ServiceException {
		final Integer id;
		if (grouping == null) {
			throw new ServiceException(
					"Tried to insert groupings with <null> param");
		}
		if (grouping.getThemes().size() == 0) {
			throw new ServiceException(
					"There are no themes defined for the grouping");
		}
		/*According to new requirements, it should be possible to save a theme without any content
		for (GroupingTheme t : grouping.getThemes()) {
			CheckGroupingData(t);
		}*/

		final SqlMapSession session = openSession();
		try {
			session.startTransaction();
			grouping.setUpdated(new Date());
			id = (Integer) session.update(getNameSpace() + ".updateGrouping",
					grouping);
			session.delete(getNameSpace() + ".deleteGroupingThemes",
					grouping.getId());

			for (GroupingTheme t : grouping.getThemes()) {
				t.setOskariGroupingId(grouping.getId());
				insertThemes(session, null, t, grouping.getId(), null);
			}

			session.commitTransaction();
		} catch (Exception e) {
			throw new ServiceException("Error updating grouping:"
					+ grouping.getName(), e);
		} finally {
			endSession(session);
		}
		return id;
	}

	@Override
	public int deleteUnbindedeMainTheme(long themeId) throws ServiceException {
		int rowCount = 0;
		final SqlMapSession session = openSession();
		try {
			session.delete(
					getNameSpace() + ".deleteUnbindedMainThemeSubThemes",
					themeId);
			rowCount = (int) session.delete(getNameSpace()
					+ ".deleteUnbindedMainTheme", themeId);

			cleanUnboundLayers(session, null);

		} catch (Exception e) {
			throw new ServiceException("Error deleting theme:" + themeId, e);
		} finally {
			try {
				// MUST be closed if explicitly opened (via openSession()).
				session.close();
			} catch (Exception ignored) {
			}
		}
		return rowCount;

	}

	@Override
	public int updateUnbindedMainTheme(GroupingTheme theme)
			throws ServiceException {
		int id;
		/*According to new requirements, it should be possible to save a theme without any content
		CheckGroupingData(theme);*/
		final SqlMapSession session = openSession();
		try {
			session.startTransaction();
			id = (Integer) session.update(getNameSpace()
					+ ".updateGroupingTheme", theme);
			session.delete(
					getNameSpace() + ".deleteUnbindedMainThemeSubThemes",
					theme.getId());
			session.delete(getNameSpace() + ".deleteThemeDataForTheme",
					theme.getId());

			for (GroupingTheme t : theme.getSubThemes()) {

				insertThemes(session, theme.getId(), t, null, theme.getId());
			}
			if (theme.getThemeData().size() > 0) {
				for (GroupingThemeData d : theme.getThemeData()) {
					d.setOskariGroupingThemeId(theme.getId());
					session.insert(getNameSpace() + ".insertGroupingThemeData",
							d);
				}
			}

			cleanUnboundLayers(session, theme);

			session.commitTransaction();
		} catch (Exception e) {
			throw new ServiceException("Error inserting main theme:"
					+ theme.getId(), e);
		} finally {
			endSession(session);
		}
		return 0;
	}

	@Override
	public long insertUnbindedMainTheme(GroupingTheme theme)
			throws ServiceException {
		final Long id;
		/*According to new requirements, it should be possible to save a theme without any content
		CheckGroupingData(theme);*/
		final SqlMapSession session = openSession();
		try {
			session.startTransaction();
			id = insertThemes(session, null, theme, null, null);
			theme.setId(id);
			cleanUnboundLayers(session, theme);

			session.commitTransaction();

		} catch (Exception e) {
			throw new ServiceException("Error inserting main theme:"
					+ theme.getId(), e);
		} finally {
			try {
				// MUST be closed if explicitly opened (via openSession()).
				session.close();
			} catch (Exception ignored) {
			}
		}
		return id;
	}

	private void cleanUnboundLayers(SqlMapSession session, GroupingTheme gt)
			throws SQLException {
		// Get id of theme which collects unbound map layers
		List<Integer> unboundLayersTheme = session.queryForList(getNameSpace()
				+ ".getUnboundLayersTheme");

		if (unboundLayersTheme != null && unboundLayersTheme.size() > 0) {
			// Get map layers which aren't bound to any general theme
			List<Integer> unboundLayerIds = session.queryForList(getNameSpace()
					+ ".getUnboundLayers");

			// Add found layers to theme which collects unbound layers
			for (Integer i : unboundLayerIds) {
				GroupingThemeData d = new GroupingThemeData();
				d.setDataId(i);
				d.setDataType(0);
				d.setOskariGroupingThemeId(unboundLayersTheme.get(0));
				session.insert(getNameSpace() + ".insertGroupingThemeData", d);
			}
		}

		if (gt != null) {
			// Remove the same data (layers or statistics) from another themes
			session.delete(getNameSpace()
					+ ".deleteUnnecessaryGroupingThemeData", gt);
		}

	}

}
