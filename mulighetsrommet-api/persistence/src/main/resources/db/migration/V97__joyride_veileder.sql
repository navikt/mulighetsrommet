create type joyride_type as enum ('OVERSIKT', 'OVERSIKTEN_LAST_STEP', 'DETALJER', 'HAR_VIST_OPPRETT_AVTALE');

create table veileder_joyride
(
    nav_ident   text                    not null,
    fullfort   boolean                 not null,
    type       joyride_type            not null,
    updated_at timestamp default now() not null,
    primary key (nav_ident, type)
);


create index veileder_joyride_har_fullfort_idx on veileder_joyride (nav_ident, type);
