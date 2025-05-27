alter table del_med_bruker
rename column veileder_tilhorer_fylke to delt_fra_fylke;

alter table del_med_bruker
rename column veileder_tilhorer_enhet to delt_fra_enhet;
