create type joyride_type as enum ('OVERSIKT', 'OVERSIKTEN_LAST_STEP', 'DETALJER', 'HAR_VIST_OPPRETT_AVTALE');

create table veileder_joyride
(
    navident   text                    not null,
    fullfort   boolean                 not null,
    type       joyride_type            not null,
    updated_at timestamp default now() not null,
    primary key (navident, type)
);


create index veileder_joyride_har_fullfort_idx on veileder_joyride (navident, type);
