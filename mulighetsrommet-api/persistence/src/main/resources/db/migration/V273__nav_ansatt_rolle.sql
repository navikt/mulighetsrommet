alter type nav_ansatt_rolle rename to rolle;

create table nav_ansatt_rolle
(
    id                   int primary key generated always as identity,
    nav_ansatt_nav_ident text  not null references nav_ansatt (nav_ident) on update cascade on delete cascade,
    rolle                rolle not null,
    unique (nav_ansatt_nav_ident, rolle)
);

with ansatt_roller as (select na.nav_ident, unnest(na.roller) as rolle from nav_ansatt na)
insert
into nav_ansatt_rolle (nav_ansatt_nav_ident, rolle)
select nav_ident, rolle
from ansatt_roller;

alter table nav_ansatt
    drop roller;
