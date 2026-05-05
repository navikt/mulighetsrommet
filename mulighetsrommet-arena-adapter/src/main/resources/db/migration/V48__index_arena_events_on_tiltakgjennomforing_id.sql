create index arena_events_tiltakgjennomforing_id_idx
    on arena_events ((payload -> 'after' ->> 'TILTAKGJENNOMFORING_ID'));
