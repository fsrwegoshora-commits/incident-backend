package com.smartincident.incidentbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-shot idempotent schema fixes that ddl-auto=update cannot handle
 * (e.g. dropping constraints that Hibernate only ever adds, never removes).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaUpdater implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropAgenciesCodeUniqueConstraint();
        addPoliceStationAgencyColumn();
    }

    /**
     * police_stations.agency_id was added to the entity after the table was created.
     * ddl-auto=update only generates the FK constraint DDL, not the column itself,
     * when the column is part of a @ManyToOne join column that Hibernate considers
     * already mapped. This migration adds the column idempotently so that every
     * query joining police_stations can read agency_id without a runtime error.
     */
    private void addPoliceStationAgencyColumn() {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE police_stations ADD COLUMN IF NOT EXISTS agency_id BIGINT"
            );
            log.info("SchemaUpdater: police_stations.agency_id column ensured");

            // Add FK constraint if missing (PostgreSQL: check pg_constraint first)
            jdbcTemplate.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint
                    WHERE conname = 'fk_police_station_agency'
                      AND conrelid = 'police_stations'::regclass
                  ) THEN
                    ALTER TABLE police_stations
                      ADD CONSTRAINT fk_police_station_agency
                      FOREIGN KEY (agency_id) REFERENCES agencies(id);
                  END IF;
                END $$;
                """);
            log.info("SchemaUpdater: police_stations → agencies FK ensured");
        } catch (Exception e) {
            log.warn("SchemaUpdater: could not add police_stations.agency_id — {}", e.getMessage());
        }
    }

    /**
     * The agencies.code column was initially unique (one agency per type).
     * The model now allows multiple agencies of the same type (POLICE, FIRE, etc.),
     * so the unique constraint must be removed.
     */
    private void dropAgenciesCodeUniqueConstraint() {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE agencies DROP CONSTRAINT IF EXISTS agencies_code_key"
            );
            log.info("SchemaUpdater: agencies_code_key constraint removed (multiple agencies per type now supported)");
        } catch (Exception e) {
            log.warn("SchemaUpdater: could not drop agencies_code_key — {}", e.getMessage());
        }
    }
}
