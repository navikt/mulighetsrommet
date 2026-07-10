create type avtale_prismodell as enum ('FORHANDSGODKJENT', 'FRI');

alter table avtale
    add prismodell avtale_prismodell;
