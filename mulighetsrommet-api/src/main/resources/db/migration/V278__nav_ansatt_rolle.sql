alter table nav_ansatt
    add created_at timestamp default now(),
    add updated_at timestamp default now();

create trigger set_timestamp
    before update
    on nav_ansatt
    for each row
execute procedure trigger_set_timestamp();

create table nav_ansatt_rolle_nav_enhet
(
    id                     integer primary key generated always as identity,
    nav_ansatt_rolle_id    integer not null references nav_ansatt_rolle on delete cascade,
    nav_enhet_enhetsnummer text    not null references nav_enhet (enhetsnummer),
    created_at             timestamp default now(),
    updated_at             timestamp default now(),
    unique (nav_ansatt_rolle_id, nav_enhet_enhetsnummer)
);

create trigger set_timestamp
    before update
    on nav_ansatt_rolle_nav_enhet
    for each row
execute procedure trigger_set_timestamp();

alter table nav_ansatt_rolle
    add created_at timestamp default now(),
    add updated_at timestamp default now();

create trigger set_timestamp
    before update
    on nav_ansatt_rolle
    for each row
execute procedure trigger_set_timestamp();
