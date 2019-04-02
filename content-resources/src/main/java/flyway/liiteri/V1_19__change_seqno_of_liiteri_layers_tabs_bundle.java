package flyway.liiteri;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/*
Changes seqno column value in the portti_view_bundle_seq of liiteri-layers-tabs bundle to place it right after hierarchical-layerlist bundle,
in order to avoid problems with populating map layer themes in 'Service package' tab when a service package is open
*/

public class V1_19__change_seqno_of_liiteri_layers_tabs_bundle implements JdbcMigration {

    private static final Logger LOG = LogFactory.getLogger(V1_19__change_seqno_of_liiteri_layers_tabs_bundle.class);

    private static final String BUNDLE_LIITERI_LAYERS_TABS = "liiteri-layers-tabs";
    private static final String BUNDLE_HIERARCHICAL_LAYERLIST = "hierarchical-layerlist";

    public void migrate(Connection connection)
            throws Exception {

        Bundle liiteriLayersTabsBundle = BundleHelper.getRegisteredBundle(BUNDLE_LIITERI_LAYERS_TABS, connection);
        if(liiteriLayersTabsBundle == null) {
            // not even registered so migration not needed
            LOG.warn(String.format("Bundle %s not registered, migration skipped", BUNDLE_LIITERI_LAYERS_TABS));
            return;
        }

        int updatedViewCount = 0;
        try {
            updatedViewCount = updateViews(connection);
        }
        finally {
        LOG.info(String.format("Seqno column value updated for %s bundle updated for %d views.",
                BUNDLE_LIITERI_LAYERS_TABS, updatedViewCount));
        }
    }

    private int updateViews(Connection connection)
            throws Exception {

        int updatedViewCount = 0;

        List<Long> list = getOutdatedViews(connection);
        LOG.info("Got", list.size(), "outdated views");

        for(Long viewId : list) {
            int newSeqno = getLiiteriLayersTabsNewSeqno(connection, viewId);
            updateLiiteriLayersTabsSeqno(connection, viewId, newSeqno);
            updatedViewCount++;
        }

        return updatedViewCount;
    }

    private List<Long> getOutdatedViews(Connection connection) throws SQLException {

        List<Long> list = new ArrayList<>();
        final String sql = "SELECT id FROM portti_view " +
                "WHERE (type = 'USER' OR type = 'DEFAULT') AND " +
                "id IN (" +
                "SELECT distinct view_id FROM portti_view_bundle_seq WHERE bundle_id IN (" +
                "SELECT id FROM portti_bundle WHERE name= ? " +
                "));";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, BUNDLE_LIITERI_LAYERS_TABS);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getLong("id"));
                }
            }
        }
        return list;
    }

    private int getLiiteriLayersTabsNewSeqno(Connection connection, long viewId)
            throws Exception{

        int hierarchicalLayerlistSeqno = getHierarchicalLayerlistSeqno(connection, viewId);
        HashSet<Integer> allSeqnosForView = getAllSeqnosForView(connection, viewId);

        int newSeqno = hierarchicalLayerlistSeqno + 1;
        while ( allSeqnosForView.contains(newSeqno)) {
            newSeqno++;
        }
        return newSeqno;
    }

    private int getHierarchicalLayerlistSeqno(Connection connection, long viewId)
            throws Exception {

        Integer seqno = null;
        final String sql = "SELECT seqno from portti_view_bundle_seq " +
                "WHERE view_id = ? AND bundle_id IN (" +
                "SELECT id FROM portti_bundle WHERE name= ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, viewId);
            statement.setString(2, BUNDLE_HIERARCHICAL_LAYERLIST);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    seqno = rs.getInt("seqno");
                }
                if (seqno == null) {
                    throw new Exception(String.format("%s bundle not added for view with id %d. This view uses %s bundle that requires %s bundle.",
                            BUNDLE_HIERARCHICAL_LAYERLIST, viewId, BUNDLE_LIITERI_LAYERS_TABS, BUNDLE_HIERARCHICAL_LAYERLIST));
                }
            }
        }
        return seqno;
    }

    private HashSet<Integer> getAllSeqnosForView(Connection connection, long viewId)
            throws Exception {

        HashSet<Integer> seqnos = new HashSet<>();
        final String sql = "SELECT seqno from portti_view_bundle_seq " +
                "WHERE view_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, viewId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    seqnos.add(rs.getInt("seqno"));
                }
            }
        }
        return seqnos;
    }

    private void updateLiiteriLayersTabsSeqno(Connection connection, long viewId, int newSeqno)
            throws SQLException {

        final String sql = "UPDATE portti_view_bundle_seq " +
                "SET seqno = ? " +
                "WHERE view_id = ? AND bundle_id IN (SELECT id FROM portti_bundle WHERE name= ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, newSeqno);
            statement.setLong(2, viewId);
            statement.setString(3, BUNDLE_LIITERI_LAYERS_TABS);

            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0){
                throw new SQLException(String.format("Updating %s seqno for view with id %d failed, no rows affected.",
                        BUNDLE_LIITERI_LAYERS_TABS, viewId));
            }
        }
    }
}