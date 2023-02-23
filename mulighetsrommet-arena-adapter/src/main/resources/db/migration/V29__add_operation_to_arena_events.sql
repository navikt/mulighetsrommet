create type arena_operation as enum ('Insert', 'Update', 'Delete');

alter table arena_events
    add column operation arena_operation;

update arena_events
set operation = case
                    when (payload ->> 'op_type') = 'I' then 'Insert'
                    when (payload ->> 'op_type') = 'U' then 'Update'
                    when (payload ->> 'op_type') = 'D' then 'Delete'
    end::arena_operation;

alter table arena_events
    alter column operation set not null;
