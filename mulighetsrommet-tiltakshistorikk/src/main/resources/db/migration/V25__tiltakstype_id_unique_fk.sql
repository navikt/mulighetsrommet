alter table tiltakstype
    add constraint tiltakstype_tiltakstype_id_unique unique (tiltakstype_id);

alter table arena_gjennomforing
    add column tiltakstype_id UUID;

update arena_gjennomforing ag
set tiltakstype_id = ts.tiltakstype_id from tiltakstype ts
where ag.arena_tiltakskode = ts.arena_tiltakskode;

alter table arena_gjennomforing
    add constraint fk_arena_gjennomforing_tiltakstype_id
        foreign key (tiltakstype_id)
            references tiltakstype (tiltakstype_id);

alter table arena_gjennomforing
    drop column arena_tiltakskode;

alter table tiltakstype
    drop constraint tiltakstype_arena_tiltakskode_key;
