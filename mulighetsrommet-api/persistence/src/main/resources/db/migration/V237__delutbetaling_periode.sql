alter table delutbetaling
    add column periode daterange;

update delutbetaling
set periode = (select periode from utbetaling where utbetaling.id = delutbetaling.utbetaling_id);

alter table delutbetaling
    alter column periode set not null;
