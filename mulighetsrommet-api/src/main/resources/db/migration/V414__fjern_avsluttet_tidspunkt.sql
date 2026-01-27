drop view if exists view_gjennomforing_gruppetiltak;

update gjennomforing set slutt_dato = avsluttet_tidspunkt::date - interval '1 DAY' where avsluttet_tidspunkt is not null;
alter table gjennomforing drop column avsluttet_tidspunkt;
