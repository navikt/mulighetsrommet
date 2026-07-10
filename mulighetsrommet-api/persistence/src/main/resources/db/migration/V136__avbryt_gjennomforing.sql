drop view if exists tiltaksgjennomforing_admin_dto_view;
drop view if exists avtale_admin_dto_view;

alter table tiltaksgjennomforing add column avbrutt_aarsak text;

update tiltaksgjennomforing set avbrutt_aarsak = 'AVBRUTT_I_ARENA' where avbrutt_tidspunkt is not null;

alter table tiltaksgjennomforing add constraint CK_AvbruttTidspunktAarsak
CHECK (
      (avbrutt_tidspunkt IS NULL AND avbrutt_aarsak IS NULL)
   OR (avbrutt_tidspunkt IS NOT NULL AND avbrutt_aarsak IS NOT NULL)
);
