alter table totrinnskontroll
    rename besluttelse to status;

update totrinnskontroll
set status = 'TIL_BEHANDLING'
where status is null;

alter table totrinnskontroll
    alter status set not null;

alter table totrinnskontroll_besluttelse_type
    rename to totrinnskontroll_status_type;
