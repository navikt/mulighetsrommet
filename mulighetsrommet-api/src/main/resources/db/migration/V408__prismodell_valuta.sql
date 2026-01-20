-- Trenger å lagre valuta for prismodeller uten satser
-- Ser for oss at man bare kan ha en valuta per prismodell
alter table prismodell
    add column valuta currency not null default 'NOK';
update prismodell
set valuta = 'NOK'::currency;
