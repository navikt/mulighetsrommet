insert into utbetaling_blokkering (utbetaling_id, blokkering)
select u.id, 'MANGLER_TILSAGN'
from utbetaling u
where u.status = 'GENERERT'
  and not exists (
    select 1
    from utbetaling_blokkering ub
    where ub.utbetaling_id = u.id
      and ub.blokkering = 'MANGLER_TILSAGN'
)
  and not exists (
    select 1
    from tilsagn t
    where t.gjennomforing_id = u.gjennomforing_id
      and t.status = 'GODKJENT'
      and t.periode && u.periode
);
