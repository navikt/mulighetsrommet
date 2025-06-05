alter type opsjonstatus rename value 'OPSJON_UTLØST' to 'OPSJON_UTLOST';
alter type opsjonstatus rename value 'SKAL_IKKE_UTLØSE_OPSJON' to 'SKAL_IKKE_UTLOSE_OPSJON';

alter type opsjonsmodell rename value 'AVTALE_UTEN_OPSJONSMODELL' to 'INGEN_OPSJONSMULIGHET';
alter type opsjonsmodell rename value 'AVTALE_VALGFRI_SLUTTDATO' to 'VALGFRI_SLUTTDATO';
