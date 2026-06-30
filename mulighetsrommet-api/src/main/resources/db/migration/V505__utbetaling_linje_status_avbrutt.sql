insert into totrinnskontroll_type (value) values ('UTBETALING_AVBRYTELSE') on conflict do nothing;
insert into utbetaling_linje_status_type (value) values ('AVBRUTT') on conflict do nothing;
