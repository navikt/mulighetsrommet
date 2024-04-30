drop view if exists avtale_admin_dto_view;

alter table avtale add column avbrutt_aarsak text;

update avtale set avbrutt_aarsak = 'AVBRUTT_I_ARENA' where avbrutt_tidspunkt is not null;

alter table avtale add constraint CK_AvbruttTidspunktAarsak
CHECK (
      (avbrutt_tidspunkt IS NULL AND avbrutt_aarsak IS NULL)
   OR (avbrutt_tidspunkt IS NOT NULL AND avbrutt_aarsak IS NOT NULL)
);
