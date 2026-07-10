alter table refusjonskrav
    add column frist_for_godkjenning timestamp not null default '2024-11-01 00:00:00'::timestamp;

alter table refusjonskrav
    alter column frist_for_godkjenning drop default;
