drop table deltaker;

create table tiltakshistorikk
(
    id                      uuid primary key,
    tiltaksgjennomforing_id uuid,
    norsk_ident             varchar(11)             not null,
    status                  deltakerstatus          not null,
    fra_dato                timestamp,
    til_dato                timestamp,
    created_at              timestamp default now() not null,
    updated_at              timestamp default now() not null,
    constraint fk_tiltaksgjennomforing foreign key (tiltaksgjennomforing_id) references tiltaksgjennomforing (id),
    beskrivelse       text,
    tiltakstypeid     uuid,
    virksomhetsnummer text
);

