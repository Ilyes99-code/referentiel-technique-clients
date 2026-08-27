package com.clinic.repository_api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.junit.jupiter.api.Test;

/**
 * Guards the mapping of AccessImage.data against the production dialect.
 *
 * This cannot be caught by running the app in the dev profile: dev is H2, where the
 * same mapping legitimately renders as a different column type. Production is
 * Flyway-managed PostgreSQL with ddl-auto=validate, so if Hibernate ever resolves
 * this field to anything other than `bytea` — which is what V5__add_access_images.sql
 * creates — the application fails schema validation at startup, i.e. at deploy time
 * and in no test.
 *
 * The failure mode is not hypothetical. Without the explicit @JdbcTypeCode on the
 * field, a declared length above the dialect's max varbinary size makes Hibernate
 * promote the type to BLOB, and BLOB on PostgreSQL is `oid`, not `bytea`.
 *
 * No database connection is involved — the type is resolved from mapping metadata.
 */
class AccessImageSchemaTest {

    @Test
    void imagePayloadMapsToByteaOnPostgres() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, PostgreSQLDialect.class.getName())
                // Nothing to connect to: resolve the dialect from the setting above
                // rather than by interrogating a live JDBC connection.
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .build();

        try {
            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(Client.class)
                    .addAnnotatedClass(AccessImage.class)
                    .buildMetadata();

            assertThat(resolveColumnType(metadata, "data"))
                    .as("payload column must be bytea — oid is what @Lob/BLOB would produce, "
                            + "and ddl-auto=validate would reject it against V5's BYTEA")
                    .isEqualTo("bytea");
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static String resolveColumnType(Metadata metadata, String propertyName) {
        PersistentClass binding = metadata.getEntityBinding(AccessImage.class.getName());
        Selectable selectable = binding.getProperty(propertyName).getValue().getSelectables().get(0);
        return ((Column) selectable).getSqlType(metadata).toLowerCase(Locale.ROOT);
    }
}
