create or replace function trigger_set_timestamp()
    returns trigger as
$$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

alter table deltaker
    add column created_at  timestamp default now() not null,
    add column updated_at  timestamp default now() not null,
    add column norsk_ident text      default null;

create trigger set_timestamp
    before update
    on deltaker
    for each row
execute procedure trigger_set_timestamp();
