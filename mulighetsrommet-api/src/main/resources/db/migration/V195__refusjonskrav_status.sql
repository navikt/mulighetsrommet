create type refusjonskrav_status as enum ('KLAR_FOR_GODKJENNING', 'GODKJENT_AV_ARRANGOR');

alter table refusjonskrav
    add column godkjent_av_arrangor_tidspunkt timestamp;
