-- Migrate del_med_bruker.sanity_id from pointing at tiltak_dokument.sanity_id
-- to pointing at tiltak_dokument.id, then rename the column and add a foreign key.

-- Step 1: Null out sanity_id for rows where it is not found in tiltak_dokument
-- they were pointing to sanity documents that has been deleted
update del_med_bruker
set sanity_id = null
where sanity_id is not null
  and not exists (
    select 1 from tiltak_dokument td
    where td.sanity_id = del_med_bruker.sanity_id
);

-- Step 2: Map existing sanity_id values to tiltak_dokument.id
update del_med_bruker
set sanity_id = td.id
from tiltak_dokument td
where del_med_bruker.sanity_id = td.sanity_id;

-- Step 3: Rename column
alter table del_med_bruker
    rename column sanity_id to tiltak_dokument_id;

-- Step 4: Add foreign key constraint
alter table del_med_bruker
    add constraint fk_del_med_bruker_tiltak_dokument
    foreign key (tiltak_dokument_id) references tiltak_dokument (id);
