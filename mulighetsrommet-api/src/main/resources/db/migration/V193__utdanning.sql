delete
from utdanning_programomrade_avtale;
delete
from utdanning_programomrade_tiltaksgjennomforing;
delete
from utdanning;
delete
from utdanning_programomrade;


alter type utdanning_sluttkompetanse rename value 'Fagbrev' to 'FAGBREV';
alter type utdanning_sluttkompetanse rename value 'Svennebrev' to 'SVENNEBREV';
alter type utdanning_sluttkompetanse rename value 'Studiekompetanse' to 'STUDIEKOMPETANSE';
alter type utdanning_sluttkompetanse rename value 'Yrkeskompetanse' to 'YRKESKOMPETANSE';

alter table utdanning
    alter column utdanning_id set not null,
    alter column utdanningsprogram set not null;
