alter table tilskudd
    add column utbetaling_id uuid references utbetaling (id);
