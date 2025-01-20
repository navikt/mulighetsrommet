create table gjennomforing_stengt_hos_arrangor
(
    id               int generated always as identity,
    gjennomforing_id uuid      not null references gjennomforing (id),
    periode          daterange not null,
    beskrivelse      text      not null,
    exclude using gist(gjennomforing_id with =, periode with &&)
);
