package flyway.liiteri;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.map.view.ViewServiceIbatisImpl;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class V1_15_2__remove_layerselection_bundle_from_default_views implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V1_15_2__remove_layerselection_bundle_from_default_views.class);

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

        Bundle layerselectionBundle = BundleHelper.getRegisteredBundle(BUNDLE_LAYERSELECTION2, conn);
        if(layerselectionBundle == null) {
            // not even registered so migration not needed
            return;
        }

        List<View> list = getOutdatedViews(conn);
        LOG.info("Got", list.size(), "outdated views");
        for(View view : list) {
            removeLayerselectionBundle(conn, layerselectionBundle, view.getId());
            updatedViewCount++;
        }
    }

    private List<View> getOutdatedViews(Connection conn) throws SQLException {

        List<View> list = new ArrayList<>();
        final String sql = "SELECT id FROM portti_view " +
                "WHERE (type = 'USER' OR type = 'DEFAULT') AND " +
                "id IN (" +
                "SELECT distinct view_id FROM portti_view_bundle_seq WHERE bundle_id IN (" +
                "SELECT id FROM portti_bundle WHERE name='layerselection2'" +
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


    public void removeLayerselectionBundle(Connection conn, Bundle layerselectionBundle, final long viewId) throws SQLException {

        // remove layerselection2 bundle
        final String sql = "DELETE FROM portti_view_bundle_seq " +
                "WHERE bundle_id = ? AND view_id=?;";

        try (PreparedStatement statement =
                     conn.prepareStatement(sql)){
            statement.setLong(1, layerselectionBundle.getBundleId());
            statement.setLong(2, viewId);
            statement.executeUpdate();
        }
    }
}