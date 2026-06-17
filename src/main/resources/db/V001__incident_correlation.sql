-- ============================================================
-- Incident Correlation Engine — one-time schema migration
-- Run this against your PostgreSQL database ONCE,
-- then restart the backend (ddl-auto=update handles the rest).
-- ============================================================

-- ── 1. New columns on the incidents table ────────────────────

ALTER TABLE incidents
    ADD COLUMN IF NOT EXISTS is_duplicate           BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS correlated_report_count INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS master_incident_id      BIGINT      REFERENCES incidents(id);

-- ── 2. Co-reporters join table ───────────────────────────────
-- Links every citizen who reported the same real-world event to the master incident.

CREATE TABLE IF NOT EXISTS incident_co_reporters (
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    PRIMARY KEY (incident_id, user_id)
);

-- ── 3. Incident correlations table ───────────────────────────
-- One row per detected correlation (DUPLICATE or RELATED).

CREATE TABLE IF NOT EXISTS incident_correlations (
    id                   BIGSERIAL    PRIMARY KEY,
    uid                  VARCHAR(50)  NOT NULL UNIQUE,

    master_incident_id   BIGINT       NOT NULL REFERENCES incidents(id),
    linked_incident_id   BIGINT                REFERENCES incidents(id),

    correlation_type     VARCHAR(20)  NOT NULL,
    confidence_score     DOUBLE PRECISION NOT NULL,
    distance_meters      DOUBLE PRECISION NOT NULL,
    time_delta_seconds   BIGINT       NOT NULL,
    auto_linked          BOOLEAN      NOT NULL DEFAULT TRUE,
    detected_at          TIMESTAMP    NOT NULL,
    dispatcher_note      TEXT,

    -- BaseEntity audit columns
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(255),
    updated_at           TIMESTAMP    DEFAULT NOW(),
    updated_by           VARCHAR(255),
    deleted_at           TIMESTAMP,
    deleted_by           VARCHAR(255),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_ic_master  ON incident_correlations (master_incident_id);
CREATE INDEX IF NOT EXISTS idx_ic_linked  ON incident_correlations (linked_incident_id);
CREATE INDEX IF NOT EXISTS idx_ic_type    ON incident_correlations (correlation_type);
CREATE INDEX IF NOT EXISTS idx_ic_active  ON incident_correlations (is_active);
