delete
from utdanning_programomrade_avtale;
delete
from utdanning_programomrade_tiltaksgjennomforing;
delete
from utdanning;
delete
from utdanning_programomrade;

alter table utdanning_programomrade_avtale
    rename to avtale_utdanningsprogram;

alter table avtale_utdanningsprogram
    rename column programomrade_id to utdanningsprogram_id;

alter table utdanning_programomrade_tiltaksgjennomforing
    rename to tiltaksgjennomforing_utdanningsprogram;

alter table tiltaksgjennomforing_utdanningsprogram
    rename column programomrade_id to utdanningsprogram_id;

alter type utdanning_program rename to utdanningsprogram_type;

alter table utdanning_programomrade
    rename to utdanningsprogram;

alter table utdanningsprogram
    rename column utdanningsprogram to utdanningsprogram_type;

alter table utdanningsprogram
    alter column nus_koder set not null;

alter table utdanning
    drop column utdanningsprogram;
