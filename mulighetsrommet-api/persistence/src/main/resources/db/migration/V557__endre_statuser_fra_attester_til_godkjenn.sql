insert into utbetaling_status_type (value)
values ('TIL_GODKJENNING');

update utbetaling
set status = 'TIL_GODKJENNING'
where status = 'TIL_ATTESTERING';

update utbetaling_avbrytelse
set returnert = 'TIL_GODKJENNING'
where returnert = 'TIL_ATTESTERING';

delete
from utbetaling_status_type
where value = 'TIL_ATTESTERING';

insert into utbetaling_linje_status_type (value)
values ('TIL_GODKJENNING');

update utbetaling_linje
set status = 'TIL_GODKJENNING'
where status = 'TIL_ATTESTERING';

delete
from utbetaling_linje_status_type
where value = 'TIL_ATTESTERING';

insert into tilskudd_behandling_status (value)
values ('TIL_GODKJENNING');

update tilskudd_behandling
set status = 'TIL_GODKJENNING'
where status = 'TIL_ATTESTERING';

delete
from tilskudd_behandling_status
where value = 'TIL_ATTESTERING';

update lagret_filter
set filter = replace(filter::text, '_TIL_ATTESTERING"', '_TIL_GODKJENNING"')::jsonb
where filter::text like '%\_TIL\_ATTESTERING"%';
