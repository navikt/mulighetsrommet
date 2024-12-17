create type tilsagn_type as enum ('TILSAGN', 'EKSTRATILSAGN');

alter table tilsagn
    add type tilsagn_type not null default 'TILSAGN';

alter table tilsagn
    alter type drop default;
