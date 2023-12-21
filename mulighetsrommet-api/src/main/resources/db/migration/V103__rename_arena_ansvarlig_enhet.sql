alter table tiltaksgjennomforing rename column arena_ansvarlig_enhet to ansvarlig_enhet;
alter table tiltaksgjennomforing alter column ansvarlig_enhet set not null;
