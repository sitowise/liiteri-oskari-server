package flyway.liiteri;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;
import org.json.JSONObject;

/*
Sets proper value of order_number column in oskari_maplayer_group_link table
to get alphabetical order (using Finnish locale) of map layers in each map layer group
*/

public class V1_18_3__order_maplayers_in_groups_alphabetically implements JdbcMigration {

    private class MapLayerData {
        private long id;
        private String finnishNameInLowerCase;

        private long getId(){
            return id;
        }

        private String getFinnishNameInLowerCase(){
            return finnishNameInLowerCase;
        }
    }

    private static final Logger LOG = LogFactory.getLogger(V1_18_3__order_maplayers_in_groups_alphabetically.class);

    public void migrate(Connection connection)
            throws Exception {

        int updatedMapLayerGroupsCount = 0;
        try {
            List<Long> mapLayerGroupIds = getMapLayerGroupIds(connection);
            for(long mapLayerGroupId : mapLayerGroupIds) {

                List<MapLayerData> orderedMapLayersInGroup = getOrderedMapLayersInGroup(connection, mapLayerGroupId);

                int index = 0;
                for(MapLayerData mapLayer : orderedMapLayersInGroup) {
                    updateMapLayerOrderNumber(connection, mapLayerGroupId, mapLayer.id, index);
                    index++;
                }
                LOG.info(String.format("Updated %d map layer links for group with id %d.", index, mapLayerGroupId));
                updatedMapLayerGroupsCount++;
            }
        }
        finally {
           LOG.info(String.format("Layer order updated for %d map layer groups.", updatedMapLayerGroupsCount));
        }
    }

    private List<Long> getMapLayerGroupIds(Connection connection)
            throws SQLException {

        List<Long> mapLayerGroups = new ArrayList<>();
        final String sql = "SELECT id FROM oskari_maplayer_group";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    mapLayerGroups.add(rs.getLong("id"));
                }
            }
        }
        LOG.info("Got", mapLayerGroups.size(), "map layer groups.");
        return mapLayerGroups;
    }

    private List<MapLayerData> getMapLayersInGroup(Connection connection, long mapLayerGroupId)
            throws Exception {

        List<MapLayerData> mapLayers = new ArrayList<>();
        final String sql = "SELECT l.maplayerid, m.locale " +
                "FROM oskari_maplayer_group_link l " +
                "LEFT OUTER JOIN oskari_maplayer m ON l.maplayerid = m.id " +
                "WHERE l.groupid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, mapLayerGroupId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    MapLayerData mapLayerData = new MapLayerData();
                    long mapLayerId =  rs.getLong(1);
                    mapLayerData.id = mapLayerId;
                    mapLayerData.finnishNameInLowerCase = getLayerFinnishNameInLowerCase(mapLayerId, rs.getString(2));
                    mapLayers.add(mapLayerData);
                }
            }
        }
        LOG.info(String.format("Got %d map layers for map layer group with id %d.",
                mapLayers.size(), mapLayerGroupId));
        return mapLayers;
    }

    private List<MapLayerData> getOrderedMapLayersInGroup(Connection connection, long mapLayerGroupId)
            throws Exception {

        List<MapLayerData> mapLayersInGroup = getMapLayersInGroup(connection, mapLayerGroupId);

        Comparator<MapLayerData> comparator = Comparator.comparing(MapLayerData::getFinnishNameInLowerCase)
                .thenComparing(MapLayerData::getId);

        return mapLayersInGroup
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private void updateMapLayerOrderNumber(Connection connection, long mapLayerGroupId, long mapLayerId, int orderNumber)
            throws SQLException {

        final String sql = "UPDATE oskari_maplayer_group_link " +
                "SET order_number = ? " +
                "WHERE mapLayerId = ? AND groupId = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderNumber);
            statement.setLong(2, mapLayerId);
            statement.setLong(3, mapLayerGroupId);

            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0){
                throw new SQLException(String.format("Updating map layer link for map layer group %d and map layer %d failed, no rows affected.",
                        mapLayerGroupId, mapLayerId));
            }
        }
    }

    private String getLayerFinnishNameInLowerCase(long mapLayerId, String locale)
            throws Exception {

        JSONObject localeObj = JSONHelper.createJSONObject(locale);

        JSONObject finnishLocaleObj = localeObj.optJSONObject("fi");
        if (finnishLocaleObj == null) {
            throw new Exception(String.format("Incorrect format of locale for map layer with id: %d. Missing \"fi\" locale.",
                    mapLayerId));
        }
        String finnishName = finnishLocaleObj.optString("name", null);
        if (finnishName == null) {
            throw new Exception(String.format("Incorrect format of locale for map layer with id: %d. Missing \"name\" property for \"fi\" locale.",
                    mapLayerId));
        }

        return finnishName.toLowerCase();
    }
}