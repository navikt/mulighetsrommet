alter table avtale
    add constraint avtale_avtale_id_key unique (avtale_id);

update avtale
set avtale_id = arena_id::int
from arena_entity_mapping aem
where aem.entity_id = avtale.id;

alter table avtale
    alter column avtale_id set not null;

alter table tiltaksgjennomforing
    add constraint tiltaksgjennomforing_avtale_id_fkey foreign key (avtale_id) references avtale (avtale_id);
