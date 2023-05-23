create table virksomhet
(
    organisasjonsnummer     text not null,
    overordnet_enhet        text references virksomhet(organisasjonsnummer),
    navn                    text not null,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    primary key (organisasjonsnummer)
);

create index virksomhet_overordnet_enhet_idx on virksomhet(overordnet_enhet);
