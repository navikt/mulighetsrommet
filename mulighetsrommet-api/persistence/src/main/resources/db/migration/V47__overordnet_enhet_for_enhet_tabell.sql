alter table enhet
    drop column enhet_id;

alter table enhet
    add constraint enhetsnummer_pk primary key (enhetsnummer);

alter table enhet
    add column overordnet_enhet text references enhet(enhetsnummer);

create index enhet_overordnet_enhet_idx on enhet(overordnet_enhet);
