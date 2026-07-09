UPDATE totrinnskontroll
    SET aarsaker = array_replace(aarsaker, 'FEIL_ANNET', 'ANNET')
WHERE aarsaker @> ARRAY['ANNET'];
