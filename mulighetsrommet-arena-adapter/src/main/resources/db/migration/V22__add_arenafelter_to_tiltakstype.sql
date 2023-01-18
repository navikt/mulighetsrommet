create type Rammeavtale as enum ('SKAL', 'KAN', 'IKKE');
create type Handlingsplan as enum ('AKT', 'LAG', 'SOK', 'TIL');
create type Administrasjonskode as enum ('AMO', 'IND', 'INST');

alter table tiltakstype
    add column tiltaksgruppekode text,
    add column administrasjonskode Administrasjonskode,
    add column send_tilsagnsbrev_til_deltaker bool,
    add column skal_ha_anskaffelsesprosess bool,
    add column maks_antall_plasser int,
    add column maks_antall_sokere int,
    add column har_fast_antall_plasser bool,
    add column skal_sjekke_antall_deltakere bool,
    add column vis_lonnstilskuddskalkulator bool,
    add column rammeavtale Rammeavtale,
    add column opplaeringsgruppe text,
    add column handlingsplan Handlingsplan,
    add column tiltaksgjennomforing_krever_sluttdato bool,
    add column maks_periode_i_mnd int,
    add column tiltaksgjennomforing_krever_meldeplikt bool,
    add column tiltaksgjennomforing_krever_vedtak bool,
    add column tiltaksgjennomforing_reservert_for_ia_bedrift bool,
    add column har_rett_paa_tilleggsstonader bool,
    add column har_rett_paa_utdanning bool,
    add column tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk bool,
    add column vis_begrunnelse_for_innsoking bool,
    add column henvisningsbrev_og_hovedbrev_til_arbeidsgiver bool,
    add column kopibrev_og_hovedbrev_til_arbeidsgiver bool;
