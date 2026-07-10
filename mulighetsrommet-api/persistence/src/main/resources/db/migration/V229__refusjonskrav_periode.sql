alter table refusjonskrav
add column periode daterange;

update refusjonskrav
set periode = (select periode from refusjonskrav_beregning_aft where id = refusjonskrav_id);

alter table refusjonskrav
alter periode set not null;
