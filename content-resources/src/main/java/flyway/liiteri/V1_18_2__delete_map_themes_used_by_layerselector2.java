package flyway.liiteri;

import fi.nls.oskari.domain.groupings.GroupingTheme;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
Deletes the map layer themes data from oskari_groupings_themes and oskari_groupings_data
that was copied to oskari_maplayer_group and oskari_maplayer_group_links by V1_18_1__populate_hierarchical_layerlist_tables Flyway
and unbinded not public map layer themes with map layer links
*/

public class V1_18_2__delete_map_themes_used_by_layerselector2 implements JdbcMigration {

    private static final Logger LOG = LogFactory.getLogger(V1_18_2__delete_map_themes_used_by_layerselector2.class);

    public void migrate(Connection connection)
            throws SQLException {

        int deletedMapLayerThemesCount = 0;
        try {
            deletedMapLayerThemesCount = deleteMapThemesWithMapLayerLinks(connection);
        }
        finally {
           LOG.info("Deleted map layer themes:", deletedMapLayerThemesCount);
        }
    }

    private int deleteMapThemesWithMapLayerLinks(Connection connection)
            throws SQLException {

        List<GroupingTheme> mapLayerThemes = getMapLayerThemes(connection);

        int index = 0;
        for(GroupingTheme theme : mapLayerThemes) {
            deleteMapLayerLinksForTheme(connection, theme.getId());
            deleteMapLayerThemes(connection, theme.getId());
            index++;
        }
        return index;
    }

    private List<GroupingTheme> getMapLayerThemes(Connection connection)
            throws SQLException {

        List<GroupingTheme> mapLayerThemes = new ArrayList<>();
        final String sql = "SELECT id FROM oskari_groupings_themes " +
                "WHERE parentthemeid IS NULL AND oskarigroupingid IS null AND mainthemeid IS null AND " +
                "themetype = 0 ";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    GroupingTheme theme = new GroupingTheme();
                    theme.setId(rs.getLong("id"));
                    mapLayerThemes.add(theme);
                }
            }
        }
        LOG.info("Got", mapLayerThemes.size(), "map layer themes to delete.");
        return mapLayerThemes;
    }

    private void deleteMapLayerLinksForTheme(Connection connection, long themeId)
            throws SQLException {

        int mapLayerLinksToDeleteCount = getMapLayerLinksForThemeCount(connection, themeId);

        final String sql = "DELETE FROM oskari_groupings_data WHERE oskarigroupingthemeid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, themeId);

            int deletedRows = statement.executeUpdate();
            if (deletedRows != mapLayerLinksToDeleteCount){
                throw new SQLException(String.format("Deleting map layer links for map layer theme %d failed, no all rows affected.",
                        themeId));
            }
            LOG.info(String.format("Deleted %d map layer links to map layer theme with id %d.", deletedRows, themeId));
        }
    }

    private int getMapLayerLinksForThemeCount(Connection connection, long themeId)
            throws SQLException {

        int mapLayersForThemeCount = 0;
        final String sql = "SELECT COUNT (*) FROM oskari_groupings_data WHERE oskarigroupingthemeid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, themeId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    mapLayersForThemeCount = rs.getInt(1);
                }
            }
        }
        return mapLayersForThemeCount;
    }

    private void deleteMapLayerThemes(Connection connection, long themeId)
            throws SQLException {

        final String sql = "DELETE FROM oskari_groupings_themes WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, themeId);

            int deletedRows = statement.executeUpdate();
            if (deletedRows == 0) {
                throw new SQLException(String.format("Deleting map layer theme with id [%s] failed, no rows affected.",
                        themeId));
            }
        }
    }
}