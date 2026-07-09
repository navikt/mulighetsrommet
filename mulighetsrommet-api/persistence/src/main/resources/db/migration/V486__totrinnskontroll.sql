-- sletter behandlinger som har blitt etterlatt etter sletting av entities
delete from totrinnskontroll
where entity_id not in (
    select id from tilsagn
    union all
    select id from utbetaling_linje
    union all
    select id from gjennomforing
    union all
    select id from tilskudd_behandling
);

insert into totrinnskontroll_type (value)
values ('TILSAGN_OPPRETTELSE'),
       ('TILSAGN_ANNULLERING'),
       ('TILSAGN_OPPGJOR'),
       ('UTBETALING_LINJE_OPPRETTELSE'),
       ('ENKELTPLASS_OKONOMI'),
       ('TILSKUDD_OPPRETTELSE');

update totrinnskontroll
set type = 'TILSAGN_ANNULLERING'
where type = 'ANNULLER';

update totrinnskontroll
set type = 'TILSAGN_OPPGJOR'
where type = 'GJOR_OPP';

update totrinnskontroll
set type = 'ENKELTPLASS_OKONOMI'
where type = 'OKONOMI';

update totrinnskontroll
set type = 'TILSAGN_OPPRETTELSE'
where type = 'OPPRETT'
  and entity_id in (select id from tilsagn);

update totrinnskontroll
set type = 'UTBETALING_LINJE_OPPRETTELSE'
where type = 'OPPRETT'
  and entity_id in (select id from utbetaling_linje);

update totrinnskontroll
set type = 'TILSKUDD_OPPRETTELSE'
where type = 'OPPRETT'
  and entity_id in (select id from tilskudd_behandling);

delete from totrinnskontroll_type
where value in ('OPPRETT', 'ANNULLER', 'GJOR_OPP', 'OKONOMI');
