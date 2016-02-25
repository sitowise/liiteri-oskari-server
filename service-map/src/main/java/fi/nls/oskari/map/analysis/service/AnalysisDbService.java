package fi.nls.oskari.map.analysis.service;

import fi.nls.oskari.domain.map.MyPlaceCategory;
import fi.nls.oskari.domain.map.UserGisData;
import fi.nls.oskari.domain.map.analysis.Analysis;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

import java.util.HashMap;
import java.util.List;

public interface AnalysisDbService extends BaseService<Analysis> {    

        public long insertAnalysisRow(final Analysis analysis);
        public int updateAnalysisCols(final Analysis analysis);
        public Analysis getAnalysisById(long id);
        public List<Analysis> getAnalysisById(List<Long> idList);
        public List<Analysis> getAnalysisByUid(String uid);
        public List<HashMap<String,Object>> getAnalysisDataByIdUid(long id, String uid, String select_body);
        public void deleteAnalysisById(final long id) throws ServiceException;
        public void deleteAnalysis(final Analysis analysis) throws ServiceException;
        public void mergeAnalysis(final Analysis analysis, final List<Long> ids) throws ServiceException;
        public int updatePublisherName(final long id, final String uuid, final String name);
        
        public List<Long> getSharedAnalysisIds(long userId);
        public List<UserGisData> getSharedAnalysis(long userId);
        public List<UserGisData> getUnexpiredAnalysis(long userId);
}
