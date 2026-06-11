alter table avtale
    drop constraint avtale_avtalenummer_key;

create index avtale_tiltakstype_id_idx on avtale (tiltakstype_id);
create index avtale_start_dato_idx on avtale (start_dato);
create index avtale_slutt_dato_idx on avtale (slutt_dato desc);
