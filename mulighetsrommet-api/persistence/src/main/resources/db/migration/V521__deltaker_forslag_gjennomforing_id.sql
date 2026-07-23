alter table deltaker_forslag
    add column gjennomforing_id uuid references gjennomforing (id);

update deltaker_forslag
set gjennomforing_id = deltaker.gjennomforing_id
from deltaker
where deltaker.id = deltaker_forslag.deltaker_id;

alter table deltaker_forslag
    alter column gjennomforing_id set not null;

create index on deltaker_forslag (gjennomforing_id);
