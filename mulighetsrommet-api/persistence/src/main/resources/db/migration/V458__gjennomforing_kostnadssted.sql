drop view if exists view_gjennomforing;

alter table gjennomforing
    add kostnadssted text;

update gjennomforing
set kostnadssted = arena_ansvarlig_enhet
where gjennomforing_type = 'ENKELTPLASS';
