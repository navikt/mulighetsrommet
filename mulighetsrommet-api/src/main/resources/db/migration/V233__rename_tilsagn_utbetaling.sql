
-- rename constraints
alter table tilsagn_utbetaling rename constraint tilsagn_utbetaling_pkey to delutbetaling_pkey;
alter table tilsagn_utbetaling rename constraint tilsagn_utbetaling_utbetaling_id_fkey to delutbetaling_utbetaling_id_fkey;
alter table tilsagn_utbetaling rename constraint tilsagn_utbetaling_tilsagn_id_fkey to delutbetaling_id_fkey;

-- rename tables
alter table tilsagn_utbetaling rename to delutbetaling;
