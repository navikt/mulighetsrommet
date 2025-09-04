import { Besluttelse, TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { getAgentDisplayName, isBesluttet } from "@/utils/totrinnskontroll";

interface BehandlerInformasjonProps {
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="4">
      <Metadata header="Behandlet av" value={getAgentDisplayName(opprettelse.behandletAv)} />
      {isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.AVVIST ? (
        <Metadata header="Returnert av" value={getAgentDisplayName(opprettelse.besluttetAv)} />
      ) : isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.GODKJENT ? (
        <Metadata header="Attestert av" value={getAgentDisplayName(opprettelse.besluttetAv)} />
      ) : null}
    </HStack>
  );
}
