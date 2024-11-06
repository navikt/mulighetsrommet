create type avvist_aarsak_type as enum ('FEIL_ANTALL_PLASSER', 'FEIL_KOSTNADSSTED', 'FEIL_PERIODE', 'FEIL_BELOP', 'FEIL_ANNET');

alter table tilsagn
add column avvist_aarsaker avvist_aarsak_type[];

alter table tilsagn
add column avvist_forklaring text;
