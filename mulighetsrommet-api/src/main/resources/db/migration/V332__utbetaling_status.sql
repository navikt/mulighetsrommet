drop type utbetaling_status;
create type utbetaling_status as enum (
    'OPPRETTET',
    'INNSENDT',
    'TIL_ATTESTERING',
    'RETURNERT',
    'FERDIG_BEHANDLET',
    'AVBRUTT'
);

alter table utbetaling add column status utbetaling_status;

update utbetaling set status = 'OPPRETTET' where id in (
    select u.id
    from utbetaling u
    left join delutbetaling d on d.utbetaling_id = u.id
    where d.utbetaling_id is null
);

update utbetaling set status = 'INNSENDT'
    where status = 'OPPRETTET' and innsender is not null;

update utbetaling u set status = 'TIL_ATTESTERING'
where exists (
  select 1 from delutbetaling d
  where d.utbetaling_id = u.id and d.status = 'TIL_ATTESTERING'
);

update utbetaling u set status = 'RETURNERT'
where exists (
  select 1 from delutbetaling d
  where d.utbetaling_id = u.id and d.status = 'RETURNERT'
);

update utbetaling u set status = 'FERDIG_BEHANDLET'
where exists (
  select 1 from delutbetaling d
  where d.utbetaling_id = u.id
    and (d.status = 'UTBETALT' or d.status = 'OVERFORT_TIL_UTBETALING')
);

alter table utbetaling
    alter column status set not null;








