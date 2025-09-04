import {
  AgentDto,
  TotrinnskontrollDto,
  TotrinnskontrollDtoBesluttet,
  TotrinnskontrollDtoTilBeslutning,
} from "@tiltaksadministrasjon/api-client";

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

export function getAgentDisplayName(agent: AgentDto): string {
  switch (agent.type) {
    case "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.NavAnsatt":
      return agent.navn || agent.navIdent;
    case "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.System":
      return agent.navn;
    case "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.Arrangor":
      return "Tiltaksarrangør";
    case undefined:
      throw new Error(`Unrecognized type of agent`);
  }
}
