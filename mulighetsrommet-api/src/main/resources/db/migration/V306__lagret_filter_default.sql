alter table lagret_filter
    add is_default boolean not null default false;

alter table lagret_filter
    alter is_default drop default
