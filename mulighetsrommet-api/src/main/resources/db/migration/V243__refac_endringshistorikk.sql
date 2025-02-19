create type document_class as enum (
    'GJENNOMFORING',
    'AVTALE',
    'TILSAGN',
    'UTBETALING'
);

alter table gjennomforing_endringshistorikk rename to endringshistorikk;
alter table endringshistorikk add column document_class document_class;

create index on endringshistorikk (document_class, document_id, sys_period);

update endringshistorikk SET document_class = 'GJENNOMFORING';
alter table endringshistorikk alter column document_class set not null;

insert into endringshistorikk (
    document_id,
    value,
    operation,
    user_id,
    sys_period,
    document_class
) select document_id, value, operation, user_id, sys_period, 'AVTALE'
from avtale_endringshistorikk;

insert into endringshistorikk (
    document_id,
    value,
    operation,
    user_id,
    sys_period,
    document_class
) select document_id, value, operation, user_id, sys_period, 'TILSAGN'
from tilsagn_endringshistorikk;

drop table tilsagn_endringshistorikk;
drop table avtale_endringshistorikk;
