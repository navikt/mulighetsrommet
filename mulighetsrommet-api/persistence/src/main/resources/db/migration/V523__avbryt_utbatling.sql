drop view if exists view_utbetaling;

insert into utbetaling_status_type values ('TIL_AVBRYTELSE');

create table utbetaling_avbrytelse
(
    utbetaling_id       uuid unique not null references utbetaling (id),
    totrinnskontroll_id uuid        not null references totrinnskontroll (id),
    returnert           text references utbetaling_status_type (value),
    godkjent            text references utbetaling_status_type (value),
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create trigger set_timestamp
    before update
    on utbetaling_avbrytelse
    for each row
execute procedure trigger_set_timestamp();
