create table nav_ansatt_rolle_nav_enhet
(
    id                     integer primary key generated always as identity,
    nav_ansatt_rolle_id    integer not null references nav_ansatt_rolle on delete cascade,
    nav_enhet_enhetsnummer text    not null references nav_enhet (enhetsnummer),
    unique (nav_ansatt_rolle_id, nav_enhet_enhetsnummer)
);

