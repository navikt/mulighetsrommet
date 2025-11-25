import { Besluttelse, TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { isBesluttet } from "@/utils/totrinnskontroll";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";

interface BehandlerInformasjonProps {
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="4">
      <MetadataVStack label="Behandlet av" value={opprettelse.behandletAv.navn} />
      {isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.AVVIST ? (
        <MetadataVStack label="Returnert av" value={opprettelse.besluttetAv.navn} />
      ) : isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.GODKJENT ? (
        <MetadataVStack label="Attestert av" value={opprettelse.besluttetAv.navn} />
      ) : null}
    </HStack>
  );
}
