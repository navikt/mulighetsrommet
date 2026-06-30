import {
  TotrinnskontrollDto,
  TotrinnskontrollDtoBeslutning,
  TotrinnskontrollDtoBesluttet,
  TotrinnskontrollDtoTilBeslutning,
} from "@tiltaksadministrasjon/api-client";

export function erSattPaVent(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoBesluttet {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning === TotrinnskontrollDtoBeslutning.SATT_PA_VENT
  );
}

export function erGodkjent(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoBesluttet {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning === TotrinnskontrollDtoBeslutning.GODKJENT
  );
}

export function erReturnert(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoBesluttet {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning === TotrinnskontrollDtoBeslutning.RETURNERT
  );
}

export function erBesluttet(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoBesluttet {
  return (
    totrinnskontroll?.type ===
    "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.Besluttet"
  );
}

export function erTilBeslutning(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoTilBeslutning {
  return (
    totrinnskontroll?.type ===
    "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.TilBeslutning"
  );
}
