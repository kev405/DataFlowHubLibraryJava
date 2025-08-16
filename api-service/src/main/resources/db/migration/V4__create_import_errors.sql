-- Errores por fila saltada durante la importación
CREATE TABLE IF NOT EXISTS import_errors (
                                             id                   BIGSERIAL PRIMARY KEY,
                                             processing_request_id UUID                     NOT NULL,
                                             row_num              BIGINT                    NULL,
                                             external_id          VARCHAR(128)              NULL,
    reason               TEXT                      NOT NULL,
    raw_line             TEXT                      NULL,
    created_at           TIMESTAMPTZ               NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS ix_import_errors_req
    ON import_errors (processing_request_id);

-- (opcional) facilita búsquedas por external_id
CREATE INDEX IF NOT EXISTS ix_import_errors_external
    ON import_errors (external_id);
