alter table delutbetaling add column frigjor_tilsagn boolean;

update delutbetaling set frigjor_tilsagn = false;
alter table delutbetaling alter frigjor_tilsagn set not null;
