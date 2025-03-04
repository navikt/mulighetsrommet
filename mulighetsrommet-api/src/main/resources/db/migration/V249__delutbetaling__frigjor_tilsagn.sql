alter table delutbetaling add column frigjor_tilsagn boolean;

update delutbetaling set frigjor_tilsagn = false where frigjor_tilsagn is null;
