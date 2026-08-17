import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { PersonIcon } from "@navikt/aksel-icons";
import { CopyButton, HStack, VStack } from "@navikt/ds-react";
import { DeltakerDto, GjennomforingEnkeltplassDto } from "@tiltaksadministrasjon/api-client";
import { DataElementStatusTag } from "@mr/frontend-common";

interface Props {
  gjennomforing: GjennomforingEnkeltplassDto;
  deltaker: DeltakerDto;
  short?: boolean;
}

export function GjennomforingEnkeltplassHeader({ gjennomforing, deltaker, short = false }: Props) {
  return (
    <VStack className="px-4 bg-ax-bg-default">
      <Separator />
      <HStack className="grid grid-flow-col auto-cols-[minmax(0,400px)] justify-start gap-4">
        <MetadataVStack
          label={
            <HStack gap="space-4" wrap={false}>
              <PersonIcon fontSize="1.5rem" />
              Navn
            </HStack>
          }
          value={deltaker.navn}
        />
        <HStack>
          <MetadataVStack
            label="Fødselsnummer"
            value={
              <HStack gap="space-4" wrap={false}>
                {deltaker.norskIdent}
                <CopyButton size="small" copyText={deltaker.norskIdent ?? ""} />
              </HStack>
            }
          />
        </HStack>
        <MetadataVStack label="Enhet" value={deltaker.oppfolgingEnhet?.navn} />
        {short && <MetadataVStack label="Veileder" value={deltaker.navVeilederNavn} />}
        {!short && (
          <>
            <MetadataVStack label="Arrangør" value={gjennomforing.arrangor.navn} />
            <MetadataVStack label="Startdato" value={formaterDato(deltaker.startDato)} />
            <MetadataVStack label="Sluttdato" value={formaterDato(deltaker.sluttDato)} />
            <MetadataVStack label="Status" value={<DataElementStatusTag {...deltaker.status} />} />
          </>
        )}
      </HStack>
      <Separator />
    </VStack>
  );
}
