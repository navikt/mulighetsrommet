-- Trenger å lagre valuta for prismodeller uten satser
-- Ser for oss at man bare kan ha en valuta per prismodell
alter table prismodell
    add column valuta currency not null default 'NOK';
alter table prismodell
    alter valuta drop default;
