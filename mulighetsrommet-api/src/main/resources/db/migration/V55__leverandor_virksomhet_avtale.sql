
create table avtale_underleverandor
(
    avtale_id           uuid                    not null
        constraint fk_avtale references avtale (id) on delete cascade,
    organisasjonsnummer text                    not null,
    created_at          timestamp default now() not null,
    updated_at          timestamp default now() not null,
    primary key (avtale_id, organisasjonsnummer)
);

create index avtale_underleverandor_orgnr_idx on avtale_underleverandor (organisasjonsnummer);
