update nav_ansatt
set roller = array_remove(
        roller || '{BESLUTTER_TILSAGN,ATTESTANT_UTBETALING,SAKSBEHANDLER_OKONOMI}', 'OKONOMI_BESLUTTER')
where 'OKONOMI_BESLUTTER' = any (roller);
