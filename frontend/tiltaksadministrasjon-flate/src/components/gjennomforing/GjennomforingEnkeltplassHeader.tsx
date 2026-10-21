import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { PersonIcon } from "@navikt/aksel-icons";
import { Box, CopyButton, HGrid, HStack, VStack } from "@navikt/ds-react";
import { DeltakerDto, GjennomforingEnkeltplassDto, Gradering } from "@tiltaksadministrasjon/api-client";
import { DataElementStatusTag } from "@mr/frontend-common";
import { GraderingTag } from "../personalia/GraderingTag";

interface Props {
  gjennomforing: GjennomforingEnkeltplassDto;
  deltaker: DeltakerDto;
  short?: boolean;
}

export function GjennomforingEnkeltplassHeader({ gjennomforing, deltaker, short = false }: Props) {
  const gradering = deltaker.gradering !== Gradering.UGRADERT;
  return (
    <Box asChild background="default">
      <VStack padding="space-8">
        <Separator />
        <HGrid gap="space-4" columns="repeat(auto-fit, minmax(10rem, 1fr))" >
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
                <HStack gap="space-4" align="center" wrap={false}>
                  {deltaker.norskIdent}
                  <CopyButton size="xsmall" copyText={deltaker.norskIdent ?? ""} />
                </HStack>
              }
            />
          </HStack>
          <MetadataVStack label="Enhet" value={deltaker.oppfolgingEnhet?.navn} />
          {short && (
            <>
              <MetadataVStack label="Veileder" value={deltaker.navVeilederNavn} />
            </>
          )}
          {!short && (
            <>
              <MetadataVStack label="Arrangør" value={gjennomforing.arrangor.navn} />
              <MetadataVStack label="Startdato" value={formaterDato(deltaker.startDato)} />
              <MetadataVStack label="Sluttdato" value={formaterDato(deltaker.sluttDato)} />
              <MetadataVStack label="Status" value={<DataElementStatusTag {...deltaker.status} />} />
            </>
          )}
          {gradering && <MetadataVStack label="Gradering" value={<GraderingTag gradering={deltaker.gradering} />} />}
        </HGrid>
        <Separator />
      </VStack>
    </Box>
  );
}
