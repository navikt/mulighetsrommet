UPDATE lagret_filter lf
SET filter = (lf.filter - 'regioner') || jsonb_build_object(
        'navEnheter',
        COALESCE(
                (
                    SELECT jsonb_agg(ne.enhetsnummer ORDER BY region.ord, ne.enhetsnummer)
                    FROM jsonb_array_elements_text(lf.filter -> 'regioner') WITH ORDINALITY AS region(value, ord)
                             JOIN nav_enhet ne
                                  ON ne.overordnet_enhet = region.value
                                      AND ne.type IN ('KO', 'ARK', 'LOKAL')
                                      AND ne.status IN ('AKTIV', 'UNDER_AVVIKLING', 'UNDER_ETABLERING')
                ),
                '[]'::jsonb
        )
                                         )
WHERE lf.filter ? 'regioner' and lf.type = 'OPPGAVE'
