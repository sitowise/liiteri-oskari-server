package flyway.liiteri;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.map.view.ViewServiceIbatisImpl;
import fi.nls.oskari.util.FlywayHelper;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class V1_16_1__add_liiteri_layers_tabs_bundle_to_views implements JdbcMigration {

    private static final Logger LOG = LogFactory.getLogger(V1_16_1__add_liiteri_layers_tabs_bundle_to_views.class);

    private static final String BUNDLE_LIITERI_LAYERS_TABS = "liiteri-layers-tabs";
    private static final String BUNDLE_HIERARCHICAL_LAYERLIST = "hierarchical-layerlist";

    private int updatedViewCount = 0;
    private ViewService service = null;

    public void migrate(Connection connection) throws Exception {
        service =  new ViewServiceIbatisImpl();
        try {
            updateViews(connection);
        }
        finally {
            LOG.info("Updated views:", updatedViewCount);
            service = null;
        }
    }

    private void updateViews(Connection connection)
            throws SQLException {

        if (!validateBundles(connection))
            return;

        List<View> list = getViews(connection);
        LOG.info("Got", list.size(), "views");
        for(View view : list) {
            FlywayHelper.addBundleWithDefaults(connection, view.getId(), BUNDLE_LIITERI_LAYERS_TABS);
            updatedViewCount++;
        }
    }

    private List<View> getViews(Connection connection) throws SQLException {

        //Get only views that have hierarchical-layerlist bundle
        List<View> list = new ArrayList<>();
        final String sql = "SELECT id FROM portti_view " +
                "WHERE (type = 'USER' OR type = 'DEFAULT') AND " +
                "id IN (" +
                "SELECT distinct view_id FROM portti_view_bundle_seq WHERE bundle_id IN (" +
                "SELECT id FROM portti_bundle WHERE name='hierarchical-layerlist'" +
                "));";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    View view = new View();
                    view.setId(rs.getLong("id"));
                    list.add(view);
                }
            }
        }
        return list;
    }


    private boolean validateBundles(Connection connection) throws SQLException {
        Bundle hierarchicalLayerListBundle = BundleHelper.getRegisteredBundle(BUNDLE_HIERARCHICAL_LAYERLIST, connection);
        if( hierarchicalLayerListBundle == null) {
            // not even registered so migration not needed
            return false;
        }
        Bundle newBundle = BundleHelper.getRegisteredBundle(BUNDLE_LIITERI_LAYERS_TABS, connection);
        if(newBundle == null) {
            throw new RuntimeException("Bundle not registered: " + BUNDLE_LIITERI_LAYERS_TABS);
        }
        return true;
    }
}