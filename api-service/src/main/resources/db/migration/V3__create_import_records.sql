-- Tabla de staging para el ejemplo (F3)
create table if not exists import_records (
                                              id                      uuid primary key,
                                              processing_request_id   uuid         not null,
                                              external_id             varchar(64)  not null,
    user_email              varchar(140) not null,
    amount                  numeric(18,2) not null,
    event_time              timestamptz   not null,
    created_at              timestamptz   not null default now()
    );

-- Idempotencia por re-ejecuciones del mismo request
create unique index if not exists ux_import_records_req_ext
    on import_records (processing_request_id, external_id);

-- Búsquedas por tiempo si aplicara
create index if not exists ix_import_records_event_time
    on import_records (event_time);
