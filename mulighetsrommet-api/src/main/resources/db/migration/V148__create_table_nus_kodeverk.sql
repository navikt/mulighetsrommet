create table nus_kodeverk(
    id text not null,
    name text not null,
    parent text not null,
    level text not null,
    version text not null,
    self_link text not null,
    primary key (id, version)
);

