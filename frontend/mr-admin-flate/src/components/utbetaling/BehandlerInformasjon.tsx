import { Besluttelse, TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { isBesluttet } from "@/utils/totrinnskontroll";
import { Metadata } from "@mr/frontend-common/components/datadriven/Metadata";

interface BehandlerInformasjonProps {
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="4">
      <Metadata label="Behandlet av" value={opprettelse.behandletAv.navn} />
      {isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.AVVIST ? (
        <Metadata label="Returnert av" value={opprettelse.besluttetAv.navn} />
      ) : isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.GODKJENT ? (
        <Metadata label="Attestert av" value={opprettelse.besluttetAv.navn} />
      ) : null}
    </HStack>
  );
}
