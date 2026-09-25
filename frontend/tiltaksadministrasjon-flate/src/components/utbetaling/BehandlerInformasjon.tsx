import { TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import {
  erGodkjent,
  erReturnert,
  utledBehandletAvNavn,
  utledBesluttetAvNavn,
} from "@/utils/totrinnskontroll";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";

interface BehandlerInformasjonProps {
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="space-16">
      <MetadataVStack label="Behandlet av" value={utledBehandletAvNavn(opprettelse)} />
      {erReturnert(opprettelse) ? (
        <MetadataVStack label="Returnert av" value={utledBesluttetAvNavn(opprettelse)} />
      ) : erGodkjent(opprettelse) ? (
        <MetadataVStack label="Attestert av" value={utledBesluttetAvNavn(opprettelse)} />
      ) : null}
    </HStack>
  );
}
