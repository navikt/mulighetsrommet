import { Besluttelse, TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { isBesluttet } from "@/utils/totrinnskontroll";

interface BehandlerInformasjonProps {
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="4">
      <Metadata header="Behandlet av" value={opprettelse.behandletAv.navn} />
      {isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.AVVIST ? (
        <Metadata header="Returnert av" value={opprettelse.besluttetAv.navn} />
      ) : isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.GODKJENT ? (
        <Metadata header="Attestert av" value={opprettelse.besluttetAv.navn} />
      ) : null}
    </HStack>
  );
}
