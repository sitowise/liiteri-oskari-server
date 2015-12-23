package fi.nls.oskari.map.layer;

import fi.nls.oskari.domain.map.UserWmsLayer;
import fi.nls.oskari.service.db.BaseService;

import java.util.List;

public interface UserWmsLayerService extends BaseService<UserWmsLayer> {

    public List<UserWmsLayer> find(final List<String> idList);

    public List<UserWmsLayer> findForUser(long id);
}
