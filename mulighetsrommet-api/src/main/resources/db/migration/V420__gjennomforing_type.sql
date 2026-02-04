create index gjennomforing_prismodell_id_idx on gjennomforing (prismodell_id);

alter type gjennomforing_type rename value 'GRUPPETILTAK' to 'AVTALE';

alter type gjennomforing_type add value 'ARENA';
