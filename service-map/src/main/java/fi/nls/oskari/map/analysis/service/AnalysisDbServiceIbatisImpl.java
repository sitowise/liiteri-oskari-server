package fi.nls.oskari.map.analysis.service;

import com.ibatis.sqlmap.client.SqlMapSession;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.mml.portti.service.db.permissions.PermissionsServiceIbatisImpl;
import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.analysis.Analysis;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.analysis.domain.AnalysisLayer;
import fi.nls.oskari.permission.domain.Resource;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseIbatisService;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisDbServiceIbatisImpl extends
        BaseIbatisService<Analysis> implements AnalysisDbService {

    private static final Logger log = LogFactory.getLogger(AnalysisDbServiceIbatisImpl.class);

    private PermissionsService permissionsService = new PermissionsServiceIbatisImpl();

    @Override
    protected String getNameSpace() {
        return "Analysis";
    }

    /*
     * The purpose of this method is to allow many SqlMapConfig.xml files in a
     * single portlet
     */
    protected String getSqlMapLocation() {
        return "META-INF/SqlMapConfig_Analysis.xml";
    }

    /**
     * insert Analysis table row
     *
     * @param analysis
     */

    public long insertAnalysisRow(final Analysis analysis) {

        log.debug("Insert analyse row:", analysis);
        final Long id = queryForObject(getNameSpace() + ".insertAnalysis", analysis);
        analysis.setId(id);
        log.debug("Got analyse id:", id);
        return id;
    }

    /**
     * update Analysis table row field mapping
     *
     * @param analysis
     */
    public int updateAnalysisCols(final Analysis analysis) {


        try {
            return getSqlMapClient().update(
                    getNameSpace() + ".updateAnalysisCols", analysis);
        } catch (SQLException e) {
            log.error(e, "Failed to update analysis col mapping", analysis);
        }
        return 0;
    }

    /**
     * Get Analysis row  by id
     *
     * @param id
     * @return analysis object
     */
    public Analysis getAnalysisById(long id) {
        return queryForObject(getNameSpace() + ".findAnalysis", id);
    }

    public List<Analysis> getAnalysisById(List<Long> idList) {
        return queryForList(getNameSpace() + ".findByIds", idList);
    }

    /**
     * Get Analysis rows of one user by uuid
     *
     * @param uid user uuid
     * @return List of analysis objects
     */
    public List<Analysis> getAnalysisByUid(String uid) {
        return queryForList(getNameSpace() + ".findAnalysisByUid", uid);
    }

    /**
     * Get Analysis data  by uuid and analysis id
     *
     * @param id analysis id
     * @param uuid user uuid
     * @param select_items select body string in select statement
     * @return List of analysis data rows
     */
    public List< HashMap<String,Object>> getAnalysisDataByIdUid(long id, String uuid, String select_items) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        params.put("uuid", uuid);
        params.put("select_items", select_items);
        return queryForList(getNameSpace() + ".findAnalysisDataByIdUid",params);
    }

    public void deleteAnalysisById(final long id) throws ServiceException {
        final Analysis analysis = getAnalysisById(id);
        deleteAnalysis(analysis);
    }


    public void deleteAnalysis(final Analysis analysis) throws ServiceException {
        if(analysis == null) {
            throw new ServiceException("Tried to delete analysis with <null> param");
        }
        final SqlMapSession session = openSession();
        try {
            session.startTransaction();
            // remove resource & permissions
            final Resource res = permissionsService.getResource(AnalysisLayer.TYPE, "analysis+" + analysis.getId());
            permissionsService.deleteResource(res);

            // remove analysis
            session.delete(getNameSpace() + ".delete-analysis-data", analysis.getId());
            session.delete(getNameSpace() + ".delete-analysis", analysis.getId());
            // style is for now 1:1 to analysis so we can delete it here
            session.delete(getNameSpace() + ".delete-analysis-style", analysis.getStyle_id());
            session.commitTransaction();
        } catch (Exception e) {
            throw new ServiceException("Error deleting analysis data with id:" + analysis.getId(), e);
        } finally {
            endSession(session);
        }
    }

    public void mergeAnalysis(final Analysis analysis, final List<Long> ids) throws ServiceException {
        if (ids == null) {
            throw new ServiceException("Tried to merge analysis with <null> param");
        }

        if (ids.size() > 1) {
            final SqlMapSession session = openSession();
            try {
                session.startTransaction();
                // replace data of old analysises to new analysis
                for (long id : ids) {
                    analysis.setOld_id(id);
                    session.update(getNameSpace() + ".merge-analysis-data", analysis);
                }
                for (long id : ids) {
                    Analysis analysis_old = queryForObject(getNameSpace() + ".findAnalysis", id);
                    session.delete(getNameSpace() + ".delete-analysis", id);
                    // style is for now 1:1 to analysis so we can delete it here
                    session.delete(getNameSpace() + ".delete-analysis-style", analysis_old.getStyle_id());
                }
                session.commitTransaction();
            } catch (Exception e) {
                throw new ServiceException("Error merging analysis data with id:" + ids.get(0), e);
            } finally {
                endSession(session);
            }
        }
    }

    /**
     * Updates a analysis publisher screenName
     *
     * @param id
     * @param uuid
     * @param name
     */
    public int updatePublisherName(final long id, final String uuid, final String name) {

        final Map<String, Object> data = new HashMap<String,Object>();
        data.put("publisher_name", name);
        data.put("uuid", uuid);
        data.put("id", id);
        try {
            return getSqlMapClient().update(
                    getNameSpace() + ".updatePublisherName", data);
        } catch (SQLException e) {
            log.error(e, "Failed to update publisher name", data);
        }
        return 0;
    }
    
    @Override
    public List<Long> getSharedAnalysisIds(long userId) {
    	List<Long> ids = new ArrayList<Long>();
    	List<String> stringIds = queryForList(getNameSpace() + ".findSharedAnalysisIds", userId);
    	for (String strId : stringIds) {
    		int pos = strId.lastIndexOf('_');
    		Long id = Long.parseLong(strId.substring(pos + 1));
    		ids.add(id);
    	}
		return ids;
	}

	@Override
	public List<UserGisData> getSharedAnalysis(long userId) {
		List<UserGisData> resultList = queryForList(getNameSpace() + ".findSharedAnalysis", userId);
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    	Date expirationDate = null;
		try {
			expirationDate = sdf.parse(sdf.format(new Date()));
		} catch (ParseException e) {
			log.error(e, "Error during date parsing"); 
		}
    	
		List<UserGisData> listToReturn = new ArrayList<UserGisData>();
		for (UserGisData u : resultList) {
			if (expirationDate.before(u.getExpirationDate())) {
				listToReturn.add(u);
			}
		}
		return listToReturn;
	}

	@Override
	public List<UserGisData> getUnexpiredAnalysis(long userId) {
		List<UserGisData> resultList = queryForList(getNameSpace() + ".findUnexpiredAnalysis", userId);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    	Date expirationDate = null;
		try {
			expirationDate = sdf.parse(sdf.format(new Date()));
		} catch (ParseException e) {
			log.error(e, "Error during date parsing"); 
		}
    	
		List<UserGisData> listToReturn = new ArrayList<UserGisData>();
		for (UserGisData u : resultList) {
			if (expirationDate.before(u.getExpirationDate())) {
				listToReturn.add(u);
			}
		}
		return listToReturn;
	}
}
