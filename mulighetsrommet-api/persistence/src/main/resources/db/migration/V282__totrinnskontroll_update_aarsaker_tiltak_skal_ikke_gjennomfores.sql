update totrinnskontroll set aarsaker = array_replace(aarsaker, 'GJENNOMFORING_AVBRYTES', 'TILTAK_SKAL_IKKE_GJENNOMFORES') where 'GJENNOMFORING_AVBRYTES'=ANY(aarsaker);
