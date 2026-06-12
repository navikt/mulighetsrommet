import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { PersonIcon } from "@navikt/aksel-icons";
import { CopyButton, HStack, VStack } from "@navikt/ds-react";
import { DeltakerDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  deltaker: DeltakerDto;
  arrangorNavn?: string;
  compact?: boolean;
}

export function DeltakerHeader({ deltaker, arrangorNavn, compact = false }: Props) {
  return (
    <VStack className="px-4 bg-ax-bg-default">
      <Separator />
      <HStack className="grid grid-flow-col auto-cols-[minmax(0,400px)] justify-start gap-4">
        <HStack>
          <PersonIcon fontSize="1.5rem" />
          <MetadataVStack label="Navn" value={deltaker.navn} />
        </HStack>
        <HStack>
          <MetadataVStack label="Fødselsnummer" value={deltaker.norskIdent} />
          <CopyButton size="small" copyText={deltaker.norskIdent ?? ""} />
        </HStack>
        <MetadataVStack label="Enhet" value={deltaker.oppfolgingEnhet?.navn} />
        {!compact && (
          <>
            <MetadataVStack label="Arrangør" value={arrangorNavn} />
            <MetadataVStack label="Startdato" value={deltaker.startDato} />
            <MetadataVStack label="Sluttdato" value={deltaker.sluttDato} />
          </>
        )}
      </HStack>
      <Separator />
    </VStack>
  );
}
