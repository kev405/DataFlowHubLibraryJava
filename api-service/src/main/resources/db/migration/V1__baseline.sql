-- Extensiones UUID (cualquiera de las dos sirve; dejamos ambas por idempotencia)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================
-- Tabla: users
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY,
    name        VARCHAR(140),
    email       VARCHAR(140) NOT NULL UNIQUE,
    role        VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

-- =========================
-- Tabla: data_files
-- =========================
CREATE TABLE IF NOT EXISTS data_files (
    id                UUID PRIMARY KEY,
    original_filename VARCHAR(255)  NOT NULL,
    storage_path      VARCHAR(1024) NOT NULL,
    size_bytes        BIGINT        NOT NULL CHECK (size_bytes > 0),
    checksum_sha256   VARCHAR(64),
    uploaded_at       TIMESTAMPTZ   NOT NULL,
    uploaded_by_id    UUID          NOT NULL REFERENCES users(id) ON DELETE RESTRICT
    );

-- =========================
-- Tabla: batch_job_configs
-- =========================
CREATE TABLE IF NOT EXISTS batch_job_configs (
    id            UUID PRIMARY KEY,
    name          VARCHAR(140)  NOT NULL,
    description   TEXT          NOT NULL DEFAULT '',
    chunk_size    INT           NOT NULL CHECK (chunk_size > 0),
    reader_type   VARCHAR(32)   NOT NULL,
    writer_type   VARCHAR(32)   NOT NULL,
    allow_restart BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ   NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE
    );

-- =========================
-- Tabla: processing_requests
-- =========================
CREATE TABLE IF NOT EXISTS processing_requests (
    id                  UUID PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    data_file_id        UUID         NOT NULL REFERENCES data_files(id) ON DELETE RESTRICT,
    parameters          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status              VARCHAR(32)  NOT NULL,              -- RequestStatus
    created_at          TIMESTAMPTZ  NOT NULL,
    requested_by_id     UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    batch_job_config_id UUID         NOT NULL REFERENCES batch_job_configs(id) ON DELETE RESTRICT
    );

-- =========================
-- Tabla: job_executions
-- =========================
CREATE TABLE IF NOT EXISTS job_executions (
    id                    UUID PRIMARY KEY,
    processing_request_id UUID         NOT NULL REFERENCES processing_requests(id) ON DELETE CASCADE,
    start_time            TIMESTAMPTZ  NOT NULL,
    end_time              TIMESTAMPTZ,
    exit_status           VARCHAR(32),       -- ExecutionStatus
    read_count            BIGINT       NOT NULL DEFAULT 0,
    write_count           BIGINT       NOT NULL DEFAULT 0,
    skip_count            BIGINT       NOT NULL DEFAULT 0,
    error_message         TEXT
    );

-- =========================
-- Tabla: reports
-- =========================
CREATE TABLE IF NOT EXISTS reports (
    id                    UUID PRIMARY KEY,
    processing_request_id UUID        NOT NULL REFERENCES processing_requests(id) ON DELETE RESTRICT,
    storage_path          VARCHAR     NOT NULL,
    summary_json          TEXT        NOT NULL,
    generated_at          TIMESTAMPTZ NOT NULL,
    generated_by_id       UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT
    );

-- Índices útiles
CREATE INDEX IF NOT EXISTS idx_pr_status_created
    ON processing_requests (status, created_at DESC);

-- “Última ejecución por request” (HU F2-06/F2-08)
CREATE INDEX IF NOT EXISTS idx_je_pr_start_desc
    ON job_executions (processing_request_id, start_time DESC);

-- (Opcional) índice GIN para buscar por clave en parameters
-- CREATE INDEX IF NOT EXISTS idx_pr_parameters_gin ON processing_requests USING GIN (parameters);
