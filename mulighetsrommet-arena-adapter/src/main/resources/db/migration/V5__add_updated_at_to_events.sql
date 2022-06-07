alter table events
    add column updated_at timestamp default now() not null;

create or replace function trigger_set_timestamp()
    returns trigger as
$$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create trigger set_timestamp
    before update
    on events
    for each row
execute procedure trigger_set_timestamp();
