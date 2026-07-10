alter table utbetaling
    add column updated_at timestamp with time zone default now() not null;

create trigger set_timestamp
    before update
    on utbetaling
    for each row
execute procedure trigger_set_timestamp();

alter table delutbetaling
    add column updated_at timestamp with time zone default now() not null;

create trigger set_timestamp
    before update
    on delutbetaling
    for each row
execute procedure trigger_set_timestamp();
