drop view if exists avtale_admin_dto_view;
drop view if exists gjennomforing_admin_dto_view;
drop view if exists tilsagn_admin_dto_view;
drop view if exists tiltakstype_admin_dto_view;
drop view if exists utbetaling_dto_view;
drop view if exists veilederflate_tiltak_view;
drop view if exists view_gjennomforing_enkeltplass_admin;
drop view if exists view_nav_ansatt_dto;

drop view if exists view_gjennomforing;
drop view if exists view_tilsagn;
drop view if exists view_veilederflate_tiltak;
alter table gjennomforing
    rename tiltaksnummer to arena_tiltaksnummer;
