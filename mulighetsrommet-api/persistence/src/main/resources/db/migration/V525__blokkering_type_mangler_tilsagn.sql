insert into utbetaling_blokkering_type (value) values ('MANGLER_TILSAGN');

alter table utbetaling_blokkering
drop constraint utbetaling_blokkering_utbetaling_id_fkey,
add column created_at timestamptz not null default now(),
add column updated_at timestamptz not null default now();

alter table utbetaling_blokkering
add constraint utbetaling_blokkering_utbetaling_id_fkey
foreign key (utbetaling_id)
references utbetaling (id)
on delete cascade;

create trigger set_timestamp
    before update
    on utbetaling_blokkering
    for each row
execute procedure trigger_set_timestamp();
