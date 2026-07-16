import {
  TotrinnskontrollDto,
  TotrinnskontrollDtoBeslutning,
} from "@tiltaksadministrasjon/api-client";

type TotrinnskontrollBesluttet = Extract<
  TotrinnskontrollDto,
  { type?: "TotrinnskontrollDto.Besluttet" }
>;

type TotrinnskontrollTilBeslutning = Extract<
  TotrinnskontrollDto,
  {
    type?: "TotrinnskontrollDto.TilBeslutning";
  }
>;

type TotrinnskontrollSattPaVent = TotrinnskontrollBesluttet & {
  beslutning: TotrinnskontrollDtoBeslutning.SATT_PA_VENT;
};

type TotrinnskontrollGodkjent = TotrinnskontrollBesluttet & {
  beslutning: TotrinnskontrollDtoBeslutning.GODKJENT;
};

type TotrinnskontrollReturnert = TotrinnskontrollBesluttet & {
  beslutning: TotrinnskontrollDtoBeslutning.RETURNERT;
};

export function erSattPaVent(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollSattPaVent {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning === TotrinnskontrollDtoBeslutning.SATT_PA_VENT
  );
}

export function erGodkjent(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollGodkjent {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning === TotrinnskontrollDtoBeslutning.GODKJENT
  );
}

export function erReturnert(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollReturnert {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning === TotrinnskontrollDtoBeslutning.RETURNERT
  );
}

export function erBesluttet(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollBesluttet {
  return totrinnskontroll?.type === "TotrinnskontrollDto.Besluttet";
}

export function erTilBeslutning(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollTilBeslutning {
  return totrinnskontroll?.type === "TotrinnskontrollDto.TilBeslutning";
}
