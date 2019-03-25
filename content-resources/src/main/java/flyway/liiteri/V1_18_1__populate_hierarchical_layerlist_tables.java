package flyway.liiteri;

import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
Creates map layer groups to be used in the hierarchical-layerlist bundle according to the map themes used in layerselector2 bundle
Assigns map layers to the newly created groups in the same way as they were assigned to the corresponding map themes
*/

public class V1_18_1__populate_hierarchical_layerlist_tables implements JdbcMigration {

    private static final Logger LOG = LogFactory.getLogger(V1_18_1__populate_hierarchical_layerlist_tables.class);

    public void migrate(Connection connection)
            throws SQLException {

        int insertedMapLayerGroupsCount = 0;
        try {
            insertedMapLayerGroupsCount = addLayerGroupsWithLayers(connection);
        }
        finally {
            LOG.info("Inserted map layer groups:", insertedMapLayerGroupsCount);
        }
    }

    private int addLayerGroupsWithLayers(Connection connection)
            throws SQLException {

        List<GroupingTheme> mapThemes = getMapThemes(connection);
        LOG.info("Got", mapThemes.size(), "map themes");

        int index = 0;
        for(GroupingTheme theme : mapThemes) {
            String locale = getLocale(theme.getName());
            long groupId = insertGroup(connection, locale, index);

            assignMapLayersToGroup(connection, theme.getId(), groupId);
            index++;
        }
        return index;
    }

    private void assignMapLayersToGroup(Connection connection, long themeId, long groupId)
            throws SQLException {

        List<Long> themeMapLayerIds = getMapLayerIdsForTheme(connection, themeId);
        LOG.info(String.format("Got %d map layers for group with id %d created from map theme with id %d.", themeMapLayerIds.size(), groupId, themeId));

        int assignedMapLayersCount = 0;
        for(Long mapLayerId : themeMapLayerIds) {
            insertLink(connection, groupId, mapLayerId);
            assignedMapLayersCount++;
        }

        LOG.info(String.format("%d map layers assigned to map layer group with id %d.", assignedMapLayersCount, groupId));
    }

    private List<Long> getMapLayerIdsForTheme(Connection connection, long themeId)
            throws SQLException {

        List<Long> themeMapLayerIds = new ArrayList<>();
        final String sql = "SELECT dataid, name FROM oskari_groupings_data " +
                "WHERE oskarigroupingthemeid = ? AND " +
                " EXISTS (SELECT 1 " +
                "              FROM oskari_maplayer " +
                "              WHERE oskari_groupings_data.dataid = oskari_maplayer.id)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, themeId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    themeMapLayerIds.add(rs.getLong("dataid"));
                }
            }
        }
        return themeMapLayerIds;
    }

    private List<GroupingTheme> getMapThemes(Connection connection)
            throws SQLException {

        List<GroupingTheme> mapThemes = new ArrayList<>();
        final String sql = "SELECT id, name FROM oskari_groupings_themes " +
                "WHERE is_public = TRUE AND " +
                "parentthemeid IS NULL AND oskarigroupingid IS null AND mainthemeid IS null AND " +
                "themetype = 0 " +
                "ORDER BY name";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    GroupingTheme theme = new GroupingTheme();
                    theme.setId(rs.getLong("id"));
                    theme.setName(rs.getString("name"));
                    mapThemes.add(theme);
                }
            }
        }
        return mapThemes;
    }

    private long insertGroup(Connection connection, String locale, int orderNumber)
            throws SQLException {

        final String sql = "INSERT INTO oskari_maplayer_group (locale, parentid, selectable, order_number) " +
                "VALUES (?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, locale);
            statement.setLong(2, -1);
            statement.setBoolean(3, true);
            statement.setLong(4, orderNumber);

            int insertedRows = statement.executeUpdate();
            if (insertedRows == 0) {
                throw new SQLException(String.format("Creating group [%s] failed, no rows affected.", locale));
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                else {
                    throw new SQLException(String.format("Creating group [%s] failed, no id obtained.", locale));
                }
            }
        }
    }

    private void insertLink(Connection connection, long groupId, long mapLayerId)
            throws SQLException {

        final String sql = "INSERT INTO oskari_maplayer_group_link (maplayerid, groupid, order_number) " +
                "VALUES (?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, mapLayerId);
            statement.setLong(2, groupId);
            statement.setLong(3, 1000000);

            int insertedRows = statement.executeUpdate();
            if (insertedRows == 0) {
                throw new SQLException(String.format("Creating map group link failed, no rows affected. Group id: %d, map layer id: %d.", groupId, mapLayerId));
            }
        }
    }

    private String getLocale(String name) {

        JSONObject nameObject = new JSONObject();
        JSONHelper.putValue(nameObject,"name", name);

        JSONObject locale = new JSONObject();
        JSONHelper.putValue(locale,"fi", nameObject);
        JSONHelper.putValue(locale,"sv", nameObject);
        JSONHelper.putValue(locale,"en", nameObject);

        return locale.toString();
    }
}