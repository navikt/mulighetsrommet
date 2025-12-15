alter table arena_deltaker
    drop constraint fk_arena_deltaker_arena_gjennomforing;

alter table arena_deltaker
    add constraint fk_arena_deltaker_arena_gjennomforing foreign key (arena_gjennomforing_id)
        references arena_gjennomforing (id) on delete cascade;

delete
from arena_gjennomforing
where arena_tiltakskode in ('ENKELAMO', 'ENKFAGYRKE', 'HOYEREUTD');
