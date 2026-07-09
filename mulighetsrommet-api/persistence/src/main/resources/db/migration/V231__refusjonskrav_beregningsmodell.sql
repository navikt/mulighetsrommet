create type beregningsmodell as enum ('FORHANDSGODKJENT', 'FRI');

alter table refusjonskrav
    add column beregningsmodell beregningsmodell;

update refusjonskrav
set beregningsmodell = 'FORHANDSGODKJENT';

alter table refusjonskrav
    alter beregningsmodell set not null;

