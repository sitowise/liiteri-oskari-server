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
import fi.nls.oskari.util.FlywayHelper;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.map.view.ViewServiceIbatisImpl;


public class V1_12__replace_oskari_hierarchical_with_layerselector2_and_layerselection2 implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V1_12__replace_oskari_hierarchical_with_layerselector2_and_layerselection2.class);

    private static final String BUNDLE_LAYERSELECTOR2 = "layerselector2";
    private static final String BUNDLE_HIERARCHICAL_LAYERLIST = "hierarchical-layerlist";
    private static final String BUNDLE_LAYERSELECTION2 = "layerselection2";

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

    private void updateViews(Connection conn)
            throws Exception {
        List<View> list = getOutdatedViews(conn);
        LOG.info("Got", list.size(), "outdated views");
        for(View view : list) {
            addLayerselector2AndLayerselection2(conn, view.getId());
            updatedViewCount++;
        }
    }

    private List<View> getOutdatedViews(Connection conn) throws SQLException {

        List<View> list = new ArrayList<>();
        final String sql = "SELECT id FROM portti_view " +
                "WHERE (type = 'USER' OR type = 'DEFAULT') AND " +
                "id IN (" +
                "SELECT distinct view_id FROM portti_view_bundle_seq WHERE bundle_id IN (" +
                "SELECT id FROM portti_bundle WHERE name='hierarchical-layerlist'" +
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


    public void addLayerselector2AndLayerselection2(Connection conn, final long viewId) throws SQLException {
        Bundle hierarchicalBundle = BundleHelper.getRegisteredBundle(BUNDLE_HIERARCHICAL_LAYERLIST, conn);
        if( hierarchicalBundle == null) {
            // not even registered so migration not needed
            return;
        }
        Bundle newBundle = BundleHelper.getRegisteredBundle(BUNDLE_LAYERSELECTOR2, conn);
        if(newBundle == null) {
            throw new RuntimeException("Bundle not registered: " + BUNDLE_LAYERSELECTOR2);
        }
        Bundle layerselectionBundle = BundleHelper.getRegisteredBundle(BUNDLE_LAYERSELECTION2, conn);
        if(layerselectionBundle == null) {
            // not even registered so migration not needed
            return;
        }

        // update hierarchical-layerlist bundle back to layerselector2
        replaceHierarchicalLayerlistBundleToLayerselector(conn, viewId, hierarchicalBundle, newBundle);
        FlywayHelper.addBundleWithDefaults(conn, viewId, BUNDLE_LAYERSELECTION2);

    }

    public void replaceHierarchicalLayerlistBundleToLayerselector(Connection conn, final long viewId, final Bundle oldBundle, final Bundle newBundle) throws SQLException {
        final String sql = "UPDATE portti_view_bundle_seq " +
                "SET " +
                "    bundle_id=?, " +
                "    startup=?, " +
                "    bundleinstance=?" +
                "WHERE bundle_id = ? and view_id=?";

        try (PreparedStatement statement =
                     conn.prepareStatement(sql)){
            statement.setLong(1, newBundle.getBundleId());
            statement.setString(2, newBundle.getStartup());
            statement.setString(3, newBundle.getName());
            statement.setLong(4, oldBundle.getBundleId());
            statement.setLong(5, viewId);
            statement.execute();
        }
    }

}
