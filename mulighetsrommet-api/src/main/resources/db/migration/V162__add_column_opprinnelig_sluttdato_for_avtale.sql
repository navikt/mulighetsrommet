alter table avtale
    add column opprinnelig_sluttdato timestamp;

update avtale
set opprinnelig_sluttdato = slutt_dato
where slutt_dato is not null;

