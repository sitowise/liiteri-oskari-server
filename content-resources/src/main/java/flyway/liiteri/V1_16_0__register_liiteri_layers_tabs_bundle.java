package flyway.liiteri;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.SQLException;

public class V1_16_0__register_liiteri_layers_tabs_bundle implements JdbcMigration {
    private static final String NAMESPACE = "liiteri";
    private static final String BUNDLE_ID = "liiteri-layers-tabs";

    public void migrate(Connection connection) throws SQLException {
        // BundleHelper checks if this bundle is already registered
        Bundle bundle = new Bundle();
        bundle.setName(BUNDLE_ID);
        bundle.setStartup(BundleHelper.getDefaultBundleStartup(NAMESPACE, BUNDLE_ID, "Liiteri layers tabs"));
        bundle.setConfig("{}");
        bundle.setState("{}");
        BundleHelper.registerBundle(bundle, connection);
    }
}