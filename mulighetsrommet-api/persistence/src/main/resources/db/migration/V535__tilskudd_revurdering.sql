-- Link mellom vedtak og en revurderingen av det

create table tilskudd_revurdering (
    tilskudd_id uuid references tilskudd(id) on delete cascade,
    tilskudd_revurdering_id uuid references tilskudd(id) on delete cascade,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null
);

create trigger set_timestamp
    before update
    on tilskudd_revurdering
    for each row
execute procedure trigger_set_timestamp();
