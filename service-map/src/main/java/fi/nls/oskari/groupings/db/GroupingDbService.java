package fi.nls.oskari.groupings.db;



import java.util.List;

import fi.nls.oskari.domain.groupings.Grouping;
import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.db.BaseService;

public interface GroupingDbService extends BaseService<Grouping> {    

    public long insertGrouping(final Grouping grouping) throws ServiceException;
    
    public int deleteGrouping(long id) throws ServiceException;

    public int updateGrouping(final Grouping grouping) throws ServiceException;
    
    public int updateUnbindedMainTheme(final GroupingTheme theme) throws ServiceException;
    
    public long insertUnbindedMainTheme(final GroupingTheme theme) throws ServiceException;   
    
    public int deleteUnbindedeMainTheme(long themeId)throws ServiceException;
    
    public List<Grouping> findByIds(List<Long> ids) throws ServiceException;
}
