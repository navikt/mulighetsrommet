alter type opsjonstatus rename value 'OPSJON_UTLØST' to 'OPSJON_UTLOST';
alter type opsjonstatus rename value 'SKAL_IKKE_UTLØSE_OPSJON' to 'SKAL_IKKE_UTLOSE_OPSJON';

alter type opsjonsmodell rename value 'AVTALE_UTEN_OPSJONSMODELL' to 'INGEN_OPSJONSMULIGHET';
alter type opsjonsmodell rename value 'AVTALE_VALGFRI_SLUTTDATO' to 'VALGFRI_SLUTTDATO';

update avtale
set opsjonsmodell = 'VALGFRI_SLUTTDATO'
where opsjonsmodell is null
  and avtaletype in ('FORHANDSGODKJENT', 'OFFENTLIG_OFFENTLIG');

update avtale
set opsjonsmodell = 'INGEN_OPSJONSMULIGHET'
where opsjonsmodell is null
  and avtaletype not in ('FORHANDSGODKJENT', 'OFFENTLIG_OFFENTLIG');

alter table avtale
    alter opsjonsmodell set not null;
