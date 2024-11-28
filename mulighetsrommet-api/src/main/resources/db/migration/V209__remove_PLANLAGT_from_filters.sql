update lagret_filter
set filter = jsonb_set(filter, '{statuser}', (filter -> 'statuser')::jsonb - 'PLANLAGT')
where type = 'Tiltaksgjennomføring'
  and filter -> 'statuser' ? 'PLANLAGT';

