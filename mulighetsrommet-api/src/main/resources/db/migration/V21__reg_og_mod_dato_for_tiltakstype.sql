alter table tiltakstype
    add column registrert_dato_i_arena timestamp,
    add column sist_endret_dato_i_arena timestamp;

update tiltakstype
set registrert_dato_i_arena = now(),
    sist_endret_dato_i_arena = now();

alter table tiltakstype
    alter column registrert_dato_i_arena set not null,
    alter column sist_endret_dato_i_arena set not null;
