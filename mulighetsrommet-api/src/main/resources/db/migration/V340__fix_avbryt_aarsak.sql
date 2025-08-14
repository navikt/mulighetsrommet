UPDATE gjennomforing
SET avbrutt_forklaring = array_to_string(avbrutt_aarsaker, ''),
    avbrutt_aarsaker   = ARRAY['ANNET']::text[]
WHERE avbrutt_aarsaker IS NOT NULL
  AND NOT (
    avbrutt_aarsaker && ARRAY['ENDRING_HOS_ARRANGOR', 'BUDSJETT_HENSYN', 'FOR_FAA_DELTAKERE', 'FEILREGISTRERING', 'AVBRUTT_I_ARENA', 'ANNET']::text[]
  );

UPDATE avtale
SET avbrutt_forklaring = array_to_string(avbrutt_aarsaker, ''),
    avbrutt_aarsaker   = ARRAY['ANNET']::text[]
WHERE avbrutt_aarsaker IS NOT NULL
  AND NOT (
    avbrutt_aarsaker && ARRAY['ENDRING_HOS_ARRANGOR', 'BUDSJETT_HENSYN', 'FOR_FAA_DELTAKERE', 'FEILREGISTRERING', 'AVBRUTT_I_ARENA', 'ANNET']::text[]
  );
