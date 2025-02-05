create type beregningsmodell as enum ('FORHANDSGODKJENT', 'FRI');

alter table refusjonskrav
add column beregningsmodell beregningsmodell;

update refusjonskrav
set beregningsmodell = (select 'FORHANDSGODKJENT' from refusjonskrav_beregning_aft where id = refusjonskrav_id);
update refusjonskrav
set beregningsmodell = (select 'FRI' from refusjonskrav_beregning_fri where id = refusjonskrav_id);

alter table refusjonskrav
alter beregningsmodell set not null;

