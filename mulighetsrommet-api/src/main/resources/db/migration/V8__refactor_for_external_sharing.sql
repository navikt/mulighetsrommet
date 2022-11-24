drop view tiltaksgjennomforing_valid;
drop table if exists innsatsgruppe cascade;
drop table if exists tiltakstype cascade;
drop table if exists tiltaksgjennomforing cascade;
drop table if exists deltaker cascade;

drop function update_tilgjengelighet();
drop function update_tilgjengelighet_after_deltakelse();

create table tiltakstype
(
    id          uuid primary key,
    navn        text                    not null,
    sanity_id   uuid,
    tiltakskode text                    not null,
    created_at  timestamp default now() not null,
    updated_at  timestamp default now() not null
);

create table tiltaksgjennomforing
(
    id             uuid primary key,
    navn           text                    not null,
    sanity_id      uuid,
    tiltakstype_id uuid                    not null,
    tiltaksnummer  text                    not null,
    created_at     timestamp default now() not null,
    updated_at     timestamp default now() not null,
    constraint fk_tiltakstype foreign key (tiltakstype_id) references tiltakstype (id)
);

create table deltaker
(
    id                      uuid primary key,
    tiltaksgjennomforing_id uuid                    not null,
    fnr                     varchar(11)             not null,
    status                  deltakerstatus          not null,
    virksomhetsnr           text                    not null,
    fra_dato                timestamp               not null,
    til_dato                timestamp,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    constraint fk_tiltaksgjennomforing foreign key (tiltaksgjennomforing_id) references tiltaksgjennomforing (id)
);
