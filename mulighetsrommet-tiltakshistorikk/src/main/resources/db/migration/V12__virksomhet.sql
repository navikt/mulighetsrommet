create table virksomhet
(
    id                                   uuid        default gen_random_uuid() not null primary key,
    created_at                           timestamptz default now(),
    updated_at                           timestamptz default now(),
    organisasjonsnummer                  text unique                           not null,
    overordnet_enhet_organisasjonsnummer text,
    navn                                 text,
    organisasjonsform                    text,
    slettet_dato                         date

);

create trigger set_timestamp
    before update
    on virksomhet
    for each row
execute procedure trigger_set_timestamp();

insert into virksomhet(organisasjonsnummer)
select distinct arrangor_organisasjonsnummer as organisasjonsnummer
from gjennomforing
union
distinct
select arrangor_organisasjonsnummer as organisasjonsnummer
from arena_deltaker;

alter table gjennomforing
    add constraint fk_gjennomforing_virksomhet
        foreign key (arrangor_organisasjonsnummer) references virksomhet (organisasjonsnummer);

create index idx_gjennomforing_arrangor_organisasjonsnummer
    on gjennomforing (arrangor_organisasjonsnummer);

alter table arena_deltaker
    add constraint fk_arena_deltaker_virksomhet
        foreign key (arrangor_organisasjonsnummer) references virksomhet (organisasjonsnummer);

create index idx_arena_deltaker_arrangor_organisasjonsnummer
    on arena_deltaker (arrangor_organisasjonsnummer);
