CREATE TABLE bruker_utbetaling (
    id                    UUID        NOT NULL PRIMARY KEY,
    sak_id                TEXT        NOT NULL,
    behandling_id         TEXT        NOT NULL,
    periode_fom           DATE        NOT NULL,
    periode_tom           DATE        NOT NULL,
    belop                 INT         NOT NULL,
    tilskuddstype         TEXT        NOT NULL,
    tiltakskode           TEXT        NOT NULL,
    hel_ved_status        TEXT,
    hel_ved_status_error  JSONB,
    saksbehandler         TEXT        NOT NULL,
    beslutter             TEXT        NOT NULL,
    besluttet_tidspunkt   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

alter table tilskudd
    add column bruker_utbetaling_id uuid references bruker_utbetaling (id);
