drop view if exists view_tilskudd_behandling;

CREATE TABLE tilskudd_behandling_status (
    value TEXT NOT NULL PRIMARY KEY
);

INSERT INTO tilskudd_behandling_status (value) VALUES
    ('TIL_GODKJENNING'),
    ('GODKJENT'),
    ('AVBRUTT'),
    ('RETURNERT');

alter table tilskudd_behandling
    add column status text references tilskudd_behandling_status(value);

update tilskudd_behandling
    set status = 'TIL_GODKJENNING';

alter table tilskudd_behandling
    alter column status set not null;
