-- rename tables
alter table refusjonskrav_beregning_fri rename to utbetaling_beregning_fri;

-- rename columns
alter table utbetaling_beregning_fri rename column refusjonskrav_id to utbetaling_id;

-- rename indexes
alter index refusjonskrav_beregning_fri_pkey rename to utbetaling_beregning_fri_pkey;

-- rename constraints
alter table utbetaling_beregning_fri rename constraint refusjonskrav_beregning_fri_refusjonskrav_id_fkey to utbetaling_beregning_fri_utbetaling_id_fkey;
