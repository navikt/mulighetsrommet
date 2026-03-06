import { Betalingsinformasjon } from "@tiltaksadministrasjon/api-client";
import { HGrid, VStack } from "@navikt/ds-react";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";

interface Props {
  betalingsinformasjon: Betalingsinformasjon;
}

export function BetalingsinformasjonDetaljer({ betalingsinformasjon }: Props) {
  switch (betalingsinformasjon.type) {
    case "BBan":
      return (
        <HGrid columns="1fr 1fr" gap="space-24">
          <MetadataVStack label="Kontonummer" value={betalingsinformasjon.kontonummer} />
          <MetadataVStack label="KID (valgfritt)" value={betalingsinformasjon.kid} />
        </HGrid>
      );
    case "IBan":
      return (
        <VStack gap="space-8">
          <MetadataVStack label="IBan" value={betalingsinformasjon.iban} />
          <MetadataVStack label="BIC/SWIFT" value={betalingsinformasjon.bic} />
          <MetadataVStack label="Banknavn" value={betalingsinformasjon.bankNavn} />
          <MetadataVStack label="Bank landkode" value={betalingsinformasjon.bankLandKode} />
        </VStack>
      );
    case undefined:
      throw Error("unreachable");
  }
}
