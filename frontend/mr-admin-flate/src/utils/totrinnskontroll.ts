import {
  TotrinnskontrollBesluttelse,
  TotrinnskontrollDto,
  TotrinnskontrollDtoBesluttet,
  TotrinnskontrollDtoTilBeslutning,
} from "@tiltaksadministrasjon/api-client";

export function isGodkjent(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoBesluttet {
  return (
    isBesluttet(totrinnskontroll) &&
    totrinnskontroll.besluttelse === TotrinnskontrollBesluttelse.GODKJENT
  );
}

export function isAvvist(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoBesluttet {
  return (
    isBesluttet(totrinnskontroll) &&
    totrinnskontroll.besluttelse === TotrinnskontrollBesluttelse.AVVIST
  );
}

export function isBesluttet(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoBesluttet {
  return (
    totrinnskontroll?.type ===
    "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.Besluttet"
  );
}

export function isTilBeslutning(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollDtoTilBeslutning {
  return (
    totrinnskontroll?.type ===
    "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.TilBeslutning"
  );
}
