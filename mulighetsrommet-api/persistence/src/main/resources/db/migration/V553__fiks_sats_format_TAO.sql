UPDATE prismodell p
SET satser = (
    SELECT jsonb_agg(
                   (elem - 'valuta') || jsonb_build_object(
                           'sats',
                           jsonb_build_object(
                                   'belop', elem->'sats',
                                   'valuta', elem->'valuta'
                           )
                                        )
                   ORDER BY ord
           )
    FROM jsonb_array_elements(p.satser) WITH ORDINALITY AS a(elem, ord)
)
WHERE p.system_id = 'TILRETTELAGT_ARBEID_ORDINAER'
  AND p.satser IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM jsonb_array_elements(p.satser) AS a(elem)
    WHERE elem->>'gjelderFra' = '2026-01-01'
      AND elem->>'sats' = '7321'
      AND elem->>'valuta' = 'NOK'
);
