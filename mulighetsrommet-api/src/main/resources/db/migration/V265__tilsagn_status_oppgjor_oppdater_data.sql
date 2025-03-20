update tilsagn set status = 'TIL_OPPGJOR' where status = 'TIL_FRIGJORING';
update tilsagn set status = 'OPPGJORT' where status = 'FRIGJORT';

update totrinnskontroll set type = 'GJOR_OPP' where type = 'FRIGJOR';

alter table delutbetaling rename column frigjor_tilsagn to gjor_opp_tilsagn;
