-- Transforms prismodell.satser from:
-- [{gjelderFra, sats: 1000, valuta}]
-- to:
-- [{gjelderFra, sats: {belop: 1000, valuta}}]

UPDATE prismodell p
SET satser = (
    SELECT COALESCE(
                   jsonb_agg(
                           CASE
                               WHEN jsonb_typeof(elem->'sats') = 'number' THEN
                                   -- remove top-level valuta, replace sats with object
                                   (elem - 'valuta')
                                       || jsonb_build_object(
                                           'sats',
                                           jsonb_build_object(
                                                   'belop', elem->'sats',
                                                   'valuta', elem->'valuta'
                                           )
                                          )
                               ELSE
                                   -- already migrated (or unexpected shape), leave as-is
                                   elem
                               END
                               ORDER BY ord
                   ),
                   '[]'::jsonb
           )
    FROM jsonb_array_elements(p.satser) WITH ORDINALITY AS a(elem, ord)
)
WHERE p.satser IS NOT NULL
  AND jsonb_typeof(p.satser) = 'array'
  AND EXISTS (
    SELECT 1
    FROM jsonb_array_elements(p.satser) e(elem)
    WHERE jsonb_typeof(elem->'sats') = 'number'
);
