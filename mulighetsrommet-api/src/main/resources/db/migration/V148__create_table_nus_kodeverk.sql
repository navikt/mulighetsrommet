create table nus_kodeverk(
    code text not null,
    name text not null,
    parent text not null,
    level text not null,
    version text not null,
    primary key (code, version)
);

