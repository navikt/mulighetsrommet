update bestilling
set status = 'SENDT'
where status = 'BESTILT';

update faktura
set status = 'SENDT'
where status = 'UTBETALT';
