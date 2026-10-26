update nav_ansatt_rolle_type
set value = 'OKONOMI_SAKSBEHANDLER_GRUPPETILTAK'
where value = 'SAKSBEHANDLER_OKONOMI';

update nav_ansatt_rolle_type
set value = 'OKONOMI_BESLUTTER_GRUPPETILTAK'
where value = 'BESLUTTER_TILSAGN';

update nav_ansatt_rolle_type
set value = 'OKONOMI_ATTESTANT_GRUPPETILTAK'
where value = 'ATTESTANT_UTBETALING';
