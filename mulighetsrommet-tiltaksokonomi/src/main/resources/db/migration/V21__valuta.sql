alter table faktura add column valuta text;
update faktura set valuta = 'NOK';

alter table faktura drop column valuta_kode;

alter table bestilling add column valuta text;
update bestilling set valuta = 'NOK';

alter table faktura alter column valuta set not null;
alter table bestilling alter column valuta set not null;

