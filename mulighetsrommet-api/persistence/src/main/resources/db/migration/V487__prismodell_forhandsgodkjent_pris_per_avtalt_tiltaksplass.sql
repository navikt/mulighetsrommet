alter type prismodell_type add value if not exists 'FORHANDSGODKJENT_PRIS_PER_AVTALT_TILTAKSPLASS';

alter type utbetaling_beregning_type add value if not exists 'FAST_SATS_PER_AVTALT_TILTAKSPLASS_PER_MANED';

create table if not exists utbetaling_tilsagn_bidrag
(
    utbetaling_id             uuid      not null references utbetaling (id) on delete cascade,
    tilsagn_id                uuid      not null references tilsagn (id),
    tilsagn_periode           daterange not null,
    tilsagn_belop             integer   not null,
    tilsagn_gjenstaende_belop integer   not null,
    bidrag_periode            daterange not null,
    bidrag_belop              integer   not null,
    primary key (utbetaling_id, tilsagn_id)
);
