create type opsjonstatus as enum ('OPSJON_UTLØST', 'SKAL_IKKE_UTLØSE_OPSJON', 'PÅGÅENDE_OPSJONSPROSESS');

create table avtale_opsjon_logg
(
    id              uuid primary key default gen_random_uuid(),
    avtale_id       uuid                    not null references avtale (id),
    sluttdato       timestamp,
    status          opsjonstatus            not null,
    registrert_dato timestamp default now() not null,
    registrert_av   text                    not null,
    created_at      timestamp default now() not null,
    updated_at      timestamp default now() not null
);
