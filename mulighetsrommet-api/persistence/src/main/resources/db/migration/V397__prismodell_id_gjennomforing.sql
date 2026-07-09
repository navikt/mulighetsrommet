DROP VIEW IF EXISTS view_gjennomforing;

alter table gjennomforing
    add column prismodell_id uuid references avtale_prismodell (id);

update gjennomforing g
set prismodell_id = a_prismodell.id
from avtale_prismodell a_prismodell
         join avtale a on a_prismodell.avtale_id = a.id
where g.avtale_id = a.id;
