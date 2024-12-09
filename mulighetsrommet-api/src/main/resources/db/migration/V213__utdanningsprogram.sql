alter table utdanningsprogram
    add column created_at timestamp default now() not null,
    add column updated_at timestamp default now() not null;

create trigger set_timestamp
    before update
    on utdanningsprogram
    for each row
execute procedure trigger_set_timestamp();
