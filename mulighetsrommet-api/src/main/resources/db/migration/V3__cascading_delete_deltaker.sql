alter table deltaker
    drop constraint fk_tiltaksgjennomforing,
    add constraint fk_tiltaksgjennomforing foreign key (tiltaksgjennomforing_id)
        references tiltaksgjennomforing (arena_id)
        on delete cascade
        on update cascade;
