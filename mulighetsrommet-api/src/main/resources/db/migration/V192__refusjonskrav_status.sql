create type refusjonskrav_status as enum ('KLAR_FOR_GODKJENNING', 'GODKJENT_AV_ARRANGOR');

alter table refusjonskrav
    add column status refusjonskrav_status not null default 'KLAR_FOR_GODKJENNING';

alter table refusjonskrav
    alter column status drop default;
