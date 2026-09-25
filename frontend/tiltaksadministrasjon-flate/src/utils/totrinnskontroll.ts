import {
  TotrinnskontrollDto,
  TotrinnskontrollDtoBesluttet,
  TotrinnskontrollDtoTilBeslutning,
  TotrinnskontrollDtoUtfall,
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
  beslutning: { utfall: TotrinnskontrollDtoUtfall.SATT_PA_VENT };
};

type TotrinnskontrollGodkjent = TotrinnskontrollBesluttet & {
  beslutning: { utfall: TotrinnskontrollDtoUtfall.GODKJENT };
};

type TotrinnskontrollReturnert = TotrinnskontrollBesluttet & {
  beslutning: { utfall: TotrinnskontrollDtoUtfall.RETURNERT };
};

export function erSattPaVent(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollSattPaVent {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning.utfall === TotrinnskontrollDtoUtfall.SATT_PA_VENT
  );
}

export function erGodkjent(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollGodkjent {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning.utfall === TotrinnskontrollDtoUtfall.GODKJENT
  );
}

export function erReturnert(
  totrinnskontroll: TotrinnskontrollDto | null,
): totrinnskontroll is TotrinnskontrollReturnert {
  return (
    erBesluttet(totrinnskontroll) &&
    totrinnskontroll.beslutning.utfall === TotrinnskontrollDtoUtfall.RETURNERT
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

export function utledBehandletAvNavn(
  totrinnskontroll: TotrinnskontrollDtoTilBeslutning | TotrinnskontrollDtoBesluttet,
) {
  return totrinnskontroll.behandling.utfortAv.navn ?? totrinnskontroll.behandling.utfortAv.agent;
}

export function utledBesluttetAvNavn(totrinnskontroll: TotrinnskontrollDtoBesluttet) {
  return totrinnskontroll.beslutning.utfortAv.navn ?? totrinnskontroll.beslutning.utfortAv.agent;
}
