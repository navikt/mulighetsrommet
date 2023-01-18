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

update tiltakstype
    set tiltaksgruppekode = '',
        administrasjonskode = 'IND',
        send_tilsagnsbrev_til_deltaker = false,
        skal_ha_anskaffelsesprosess = false,
        vis_lonnstilskuddskalkulator = false,
        tiltaksgjennomforing_krever_sluttdato = false,
        tiltaksgjennomforing_krever_vedtak = false,
        tiltaksgjennomforing_reservert_for_ia_bedrift = false,
        har_rett_paa_tilleggsstonader = false,
        har_rett_paa_utdanning = false,
        tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk = false,
        vis_begrunnelse_for_innsoking = false,
        henvisningsbrev_og_hovedbrev_til_arbeidsgiver = false,
        kopibrev_og_hovedbrev_til_arbeidsgiver = false;

alter table tiltakstype
    alter column tiltaksgruppekode set not null,
    alter column administrasjonskode set not null,
    alter column send_tilsagnsbrev_til_deltaker set not null,
    alter column skal_ha_anskaffelsesprosess set not null,
    alter column vis_lonnstilskuddskalkulator set not null,
    alter column tiltaksgjennomforing_krever_sluttdato set not null,
    alter column tiltaksgjennomforing_krever_vedtak set not null,
    alter column tiltaksgjennomforing_reservert_for_ia_bedrift set not null,
    alter column har_rett_paa_tilleggsstonader set not null,
    alter column har_rett_paa_utdanning set not null,
    alter column tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk set not null,
    alter column vis_begrunnelse_for_innsoking set not null,
    alter column henvisningsbrev_og_hovedbrev_til_arbeidsgiver set not null,
    alter column kopibrev_og_hovedbrev_til_arbeidsgiver set not null;
