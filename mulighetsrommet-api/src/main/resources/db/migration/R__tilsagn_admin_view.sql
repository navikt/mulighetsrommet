drop view if exists tilsagn_admin_dto_view;

create view tilsagn_admin_dto_view as
select
    tilsagn.id,
    tilsagn.tiltaksgjennomforing_id,
    tilsagn.periode_start,
    tilsagn.periode_slutt,
    tilsagn.beregning,
    tilsagn.annullert_tidspunkt,
    tilsagn.lopenummer,
    tilsagn.kostnadssted,
    tilsagn.opprettet_av,
    tilsagn.besluttelse,
    tilsagn.besluttet_tidspunkt,
    tilsagn.besluttet_av,
    tilsagn.avvist_aarsaker,
    tilsagn.avvist_forklaring,
    nav_enhet.navn              as kostnadssted_navn,
    nav_enhet.overordnet_enhet  as kostnadssted_overordnet_enhet,
    nav_enhet.type              as kostnadssted_type,
    nav_enhet.status            as kostnadssted_status,
    arrangor.id                         as arrangor_id,
    arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
    arrangor.navn                       as arrangor_navn,
    arrangor.slettet_dato is not null   as arrangor_slettet,
    t.antall_plasser as antall_plasser,
    t.tiltaksnummer as tiltaksnummer
from tilsagn
         inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
         inner join arrangor on arrangor.id = tilsagn.arrangor_id
         inner join tiltaksgjennomforing t on t.id = tilsagn.tiltaksgjennomforing_id
