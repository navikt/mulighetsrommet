drop view if exists view_nav_ansatt;

alter table nav_ansatt_rolle
    alter rolle type text;

drop type rolle;

create table nav_ansatt_rolle_type
(
    value text not null primary key
);

insert into nav_ansatt_rolle_type
values ('TEAM_MULIGHETSROMMET'),
       ('TILTAKADMINISTRASJON_GENERELL'),
       ('TILTAKSTYPER_SKRIV'),
       ('TILTAKSGJENNOMFORINGER_SKRIV'),
       ('OPPFOLGER_GJENNOMFORING'),
       ('AVTALER_SKRIV'),
       ('SAKSBEHANDLER_OKONOMI'),
       ('OKONOMI_LES'),
       ('BESLUTTER_TILSAGN'),
       ('ATTESTANT_UTBETALING'),
       ('KONTAKTPERSON');

alter table nav_ansatt_rolle
    add foreign key (rolle) references nav_ansatt_rolle_type (value) on update cascade;
