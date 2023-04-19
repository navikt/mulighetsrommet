alter table enhet
    add column overordnet_enhet text;

create index enhet_overordnet_enhet_idx on enhet(overordnet_enhet);
