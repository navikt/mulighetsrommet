create table if not exists tiltaksgjennomforing_endringshistorikk
(
    document_id uuid      not null,
    value       jsonb     not null,
    operation   text      not null,
    user_id     text      not null,
    sys_period  tstzrange not null
);

create index on tiltaksgjennomforing_endringshistorikk (document_id, sys_period);
create index on tiltaksgjennomforing_endringshistorikk using gist (sys_period) include (document_id);

create table if not exists avtale_endringshistorikk
(
    document_id uuid      not null,
    value       jsonb     not null,
    operation   text      not null,
    user_id     text      not null,
    sys_period  tstzrange not null
);

create index on avtale_endringshistorikk (document_id, sys_period);
create index on avtale_endringshistorikk using gist (sys_period) include (document_id);
