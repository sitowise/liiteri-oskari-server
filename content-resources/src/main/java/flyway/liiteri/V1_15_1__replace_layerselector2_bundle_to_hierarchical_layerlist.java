package flyway.liiteri;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.map.view.ViewServiceIbatisImpl;


public class V1_15_1__replace_layerselector2_bundle_to_hierarchical_layerlist implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V1_15_1__replace_layerselector2_bundle_to_hierarchical_layerlist.class);

    private static final String BUNDLE_LAYERSELECTOR2 = "layerselector2";
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
            throws Exception {

        Bundle layerselectorBundle = BundleHelper.getRegisteredBundle(BUNDLE_LAYERSELECTOR2, connection);
        if( layerselectorBundle == null) {
            // not even registered so migration not needed
            return;
        }
        Bundle hierarchicalLayerListBundle = BundleHelper.getRegisteredBundle(BUNDLE_HIERARCHICAL_LAYERLIST, connection);
        if(hierarchicalLayerListBundle == null) {
            throw new RuntimeException("Bundle not registered: " + BUNDLE_HIERARCHICAL_LAYERLIST);
        }

        List<View> list = getOutdatedViews(connection);
        LOG.info("Got", list.size(), "outdated views");
        for(View view : list) {
            replaceLayerselectorBundleToHierarchicalLayerlist(connection, view.getId(), layerselectorBundle, hierarchicalLayerListBundle);
            updatedViewCount++;
        }
    }

    private List<View> getOutdatedViews(Connection conn) throws SQLException {

        List<View> list = new ArrayList<>();
        final String sql = "SELECT id FROM portti_view " +
                "WHERE (type = 'USER' OR type = 'DEFAULT') AND " +
                "id IN (" +
                "SELECT distinct view_id FROM portti_view_bundle_seq WHERE bundle_id IN (" +
                "SELECT id FROM portti_bundle WHERE name='layerselection2' OR name='layerselector2'" +
                "));";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
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

    private void replaceLayerselectorBundleToHierarchicalLayerlist(Connection conn, final long viewId, final Bundle oldBundle, final Bundle newBundle) throws SQLException {
        final String sql = "UPDATE portti_view_bundle_seq " +
                "SET " +
                "    bundle_id=?, " +
                "    startup=?, " +
                "    config=?, " +
                "    state=?, " +
                "    bundleinstance=?" +
                "WHERE bundle_id = ? and view_id=?";

        try (PreparedStatement statement =
                     conn.prepareStatement(sql)){
            statement.setLong(1, newBundle.getBundleId());
            statement.setString(2, newBundle.getStartup());
            statement.setString(3, newBundle.getConfig());
            statement.setString(4, newBundle.getState());
            statement.setString(5, newBundle.getName());
            statement.setLong(6, oldBundle.getBundleId());
            statement.setLong(7, viewId);
            statement.execute();
        }
    }
}