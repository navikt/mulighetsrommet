update tiltakstype
set opprettet_tidspunkt = created_at,
    oppdatert_tidspunkt = updated_at
where oppdatert_tidspunkt is null
  and oppdatert_tidspunkt is null;

alter table tiltakstype
    alter column opprettet_tidspunkt set not null,
    alter column oppdatert_tidspunkt set not null;

alter table gjennomforing
    alter column opprettet_tidspunkt set not null,
    alter column oppdatert_tidspunkt set not null;
