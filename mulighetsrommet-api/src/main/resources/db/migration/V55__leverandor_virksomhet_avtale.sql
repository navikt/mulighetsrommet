create type virksomhetstype as enum ('Hovedenhet', 'Underenhet');

create table leverandor_virksomhet_avtale
(
    avtale_id           uuid                    not null
        constraint fk_avtale references avtale (id) on delete cascade,
    organisasjonsnummer text                    not null,
    type_virksomhet     virksomhetstype         not null,
    created_at          timestamp default now() not null,
    updated_at          timestamp default now() not null,
    primary key (avtale_id, organisasjonsnummer)
);

create index leverandor_virksomhet_avtale_orgnr_idx on leverandor_virksomhet_avtale (organisasjonsnummer);
