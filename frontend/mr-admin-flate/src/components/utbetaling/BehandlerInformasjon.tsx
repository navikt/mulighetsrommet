import { TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { isAvvist, isGodkjent } from "@/utils/totrinnskontroll";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";

interface BehandlerInformasjonProps {
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="space-16">
      <MetadataVStack label="Behandlet av" value={opprettelse.behandletAv.navn} />
      {isAvvist(opprettelse) ? (
        <MetadataVStack label="Returnert av" value={opprettelse.besluttetAv.navn} />
      ) : isGodkjent(opprettelse) ? (
        <MetadataVStack label="Attestert av" value={opprettelse.besluttetAv.navn} />
      ) : null}
    </HStack>
  );
}
