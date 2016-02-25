package fi.nls.oskari.map.layer;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.LayerGroup;
import fi.nls.oskari.service.db.BaseIbatisService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 * User: SMAKINEN
 * Date: 16.12.2013
 * Time: 13:43
 * To change this template use File | Settings | File Templates.
 */
public class LayerGroupServiceIbatisImpl extends BaseIbatisService<LayerGroup> implements LayerGroupService {

    // FIXME: use some caching lib for this, and clear cache on update/delete etc
    final private static Map<Integer, LayerGroup> ID_CACHE = new HashMap<Integer, LayerGroup>();

    public LayerGroupServiceIbatisImpl() {
        // populate cache on startup
        try {
            findAll();
        }
        catch (Exception ignored) {
            // just for caching, exception catched so tests would be easier to implement
        }
    }

    public boolean hasPermissionToUpdate(final User user, final int layerId) {

        // TODO: check against permissions
        if (!user.isAdmin()) {
            return false;
        }
        if (layerId <= -1) {
            return false;
        }
        // TODO: maybe check if we have a layer with given id in DB
        return true;
    }

    @Override
    protected String getNameSpace() {
        return "LayerGroup";
    }

    public LayerGroup find(int id) {
        if(id == -1) {
            return null;
        }
        LayerGroup group = ID_CACHE.get(id);
        if(group != null) {
            return group;
        }
        group = super.find(id);
        if(group != null) {
            ID_CACHE.put(group.getId(), group);
        }
        return group;
    }

    /**
     * Returns first group (searched in arbitratry order) that has any part
     * of the group name in any language(!) matching the given name-parameter.
     * Use carefully and preferrably with long parameter name.
     * FIXME: Quick and dirty
     * @param name
     * @return matching group or null if no match
     */
    public LayerGroup findByName(final String name) {
        final List<LayerGroup> groups = findAll();
        for(LayerGroup group : groups) {
            if(group.getLocale().toString().indexOf(name) > -1) {
                return group;
            }
        }
        return null;
    }

    public List<LayerGroup> findAll() {
        final List<LayerGroup> groups = super.findAll();
        for(LayerGroup group : groups) {
            ID_CACHE.put(group.getId(), group);
        }
        return groups;
    }

    public void delete(int id) {
        ID_CACHE.remove(id);
        super.delete(id);
    }

    public void update(final LayerGroup group) {
        ID_CACHE.put(group.getId(), group);
        super.update(group);
    }
}
