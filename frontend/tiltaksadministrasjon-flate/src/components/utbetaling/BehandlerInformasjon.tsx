import { TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { erReturnert, erGodkjent } from "@/utils/totrinnskontroll";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";

interface BehandlerInformasjonProps {
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="space-16">
      <MetadataVStack
        label="Behandlet av"
        value={opprettelse.behandletAv.navn || opprettelse.behandletAv.agent}
      />
      {erReturnert(opprettelse) ? (
        <MetadataVStack
          label="Returnert av"
          value={opprettelse.besluttetAv.navn || opprettelse.behandletAv.agent}
        />
      ) : erGodkjent(opprettelse) ? (
        <MetadataVStack
          label="Attestert av"
          value={opprettelse.besluttetAv.navn || opprettelse.behandletAv.agent}
        />
      ) : null}
    </HStack>
  );
}
