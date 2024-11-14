alter table refusjonskrav_beregning_aft
    alter column periode type daterange using daterange(lower(periode)::date, upper(periode)::date);

alter table refusjonskrav_deltakelse_periode
    alter column periode type daterange using daterange(lower(periode)::date, upper(periode)::date);
