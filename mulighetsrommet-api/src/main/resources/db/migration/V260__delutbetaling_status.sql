create type delutbetaling_status as enum ('TIL_GODKJENNING', 'GODKJENT', 'RETURNERT', 'UTBETALT');

alter table delutbetaling
    add column status delutbetaling_status;

update delutbetaling
set status = 'UTBETALT'
where delutbetaling.sendt_til_okonomi_tidspunkt is not null;

update delutbetaling
set status = 'TIL_GODKJENNING'
where status is null;

alter table delutbetaling
    alter column status set not null;
