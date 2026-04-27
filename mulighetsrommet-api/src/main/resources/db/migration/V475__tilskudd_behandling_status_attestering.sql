drop view if exists view_tilskudd_behandling;

insert into tilskudd_behandling_status (value) values ('TIL_ATTESTERING'), ('FERDIG_BEHANDLET');

update tilskudd_behandling set status = 'TIL_ATTESTERING' where status = 'TIL_GODKJENNING';
update tilskudd_behandling set status = 'FERDIG_BEHANDLET' where status = 'GODKJENT';

delete from tilskudd_behandling_status where value in ('TIL_GODKJENNING', 'GODKJENT');


