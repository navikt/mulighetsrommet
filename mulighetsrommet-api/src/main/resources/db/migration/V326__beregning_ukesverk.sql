alter type utbetaling_beregning_type add value 'PRIS_PER_UKESVERK';

create table utbetaling_deltakelse_ukesverk
(
    utbetaling_id uuid references utbetaling (id) not null,
    deltakelse_id uuid                            not null,
    ukesverk      numeric(5, 2)                   not null,
    unique (utbetaling_id, deltakelse_id)
);
