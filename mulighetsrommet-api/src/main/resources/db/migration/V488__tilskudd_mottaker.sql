update tilskudd set utbetaling_mottaker = 'ARRANGOR' where utbetaling_mottaker = 'arrangor';
update tilskudd set utbetaling_mottaker = 'BRUKER' where utbetaling_mottaker = 'bruker';

alter table tilskudd
    add column utbetaling_id uuid references utbetaling (id);
