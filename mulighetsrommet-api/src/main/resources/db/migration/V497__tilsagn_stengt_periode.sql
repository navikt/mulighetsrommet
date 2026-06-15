create table tilsagn_stengt_periode
(
    tilsagn_id  uuid      not null references tilsagn (id) on delete cascade,
    periode     daterange not null,
    beskrivelse text      not null,
    exclude using gist(tilsagn_id with =, periode with &&)
);
