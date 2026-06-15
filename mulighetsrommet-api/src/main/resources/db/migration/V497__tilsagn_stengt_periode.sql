alter table tilsagn
    add column beregning_stengte_perioder jsonb not null default '[]';
