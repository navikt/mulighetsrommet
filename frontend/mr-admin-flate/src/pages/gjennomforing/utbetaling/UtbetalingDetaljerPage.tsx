import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import {
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingStatusDtoType,
} from "@tiltaksadministrasjon/api-client";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Box, CopyButton, Heading, HelpText, HGrid, HStack, Link, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import UtbetalingBeregningView from "@/components/utbetaling/beregning/UtbetalingBeregningView";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import {
  useUtbetaling,
  useUtbetalingBeregning,
  useUtbetalingEndringshistorikk,
  useUtbetalingsLinjer,
} from "./utbetalingPageLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { BesluttUtbetalingLinjeView } from "@/components/utbetaling/BesluttUtbetalingLinjeView";
import { RedigerUtbetalingLinjeView } from "@/components/utbetaling/RedigerUtbetalingLinjeView";
import {
  MetadataFritekstfelt,
  MetadataVStack,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { BetalingsinformasjonDetaljer } from "@/components/utbetaling/BetalingsinformasjonDetaljer";

function useUtbetalingDetaljerData() {
  const { utbetalingId } = useRequiredParams(["utbetalingId"]);

  const { data: historikk } = useUtbetalingEndringshistorikk(utbetalingId);
  const { utbetaling, handlinger } = useUtbetaling(utbetalingId);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter: [] }, utbetalingId);

  return { historikk, utbetaling, handlinger, beregning };
}

export function UtbetalingDetaljerPage() {
  const { historikk, utbetaling, handlinger, beregning } = useUtbetalingDetaljerData();

  return (
    <VStack gap="space-12">
      <HGrid columns="1fr auto" align="start">
        <TwoColumnGrid separator>
          <Box>
            <Heading size="medium" spacing level="3" data-testid="utbetaling-til-utbetaling">
              Detaljer
            </Heading>
            <VStack gap="space-16">
              <MetadataVStack
                label={utbetalingTekster.metadata.status}
                value={<UtbetalingStatusTag status={utbetaling.status} />}
              />
              {utbetaling.avbruttBegrunnelse && (
                <MetadataFritekstfelt
                  label={utbetalingTekster.metadata.avbruttBegrunnelse}
                  value={utbetaling.avbruttBegrunnelse}
                />
              )}
              <MetadataVStack
                label={utbetalingTekster.metadata.periode}
                value={formaterPeriode(utbetaling.periode)}
              />
              <MetadataVStack
                label={utbetalingTekster.beregning.belop.label}
                value={formaterValutaBelop(utbetaling.beregning)}
              />
              {utbetaling.type.tagName && (
                <HGrid columns="1fr 1fr" gap="space-24">
                  <MetadataVStack
                    label={utbetalingTekster.metadata.type}
                    value={
                      <HStack gap="space-4">
                        {utbetaling.type.displayName}
                        <UtbetalingTypeTag type={utbetaling.type.displayName} />
                      </HStack>
                    }
                  />
                  {utbetaling.korreksjon?.opprinneligUtbetaling && (
                    <MetadataVStack
                      label={utbetalingTekster.korreksjon.gjelderUtbetaling}
                      value={
                        <Link
                          as={ReactRouterLink}
                          to={`/gjennomforinger/${utbetaling.gjennomforingId}/utbetalinger/${utbetaling.korreksjon.opprinneligUtbetaling}`}
                        >
                          Opprinnelig utbetaling
                        </Link>
                      }
                    />
                  )}
                </HGrid>
              )}
              {utbetaling.korreksjon && (
                <MetadataFritekstfelt
                  label={utbetalingTekster.korreksjon.begrunnelse}
                  value={utbetaling.korreksjon.begrunnelse}
                />
              )}
              {utbetaling.utbetalesTidligstDato && (
                <MetadataVStack
                  label={
                    <HStack align="center" gap="space-4">
                      {utbetalingTekster.metadata.utbetalesTidligstDato}
                      <HelpText>{utbetalingTekster.metadata.utbetalesTidligstDatoHelpText}</HelpText>
                    </HStack>
                  }
                  value={formaterDato(utbetaling.utbetalesTidligstDato)}
                />
              )}
            </VStack>
          </Box>
          <Box>
            <Heading size="medium" level="3" spacing>
              Betalingsinformasjon
            </Heading>
            <VStack gap="space-16">
              {utbetaling.betalingsinformasjon && (
                <BetalingsinformasjonDetaljer
                  betalingsinformasjon={utbetaling.betalingsinformasjon}
                />
              )}
              {utbetaling.journalpostId && (
                <MetadataVStack
                  label="Journalpost-ID i Gosys"
                  value={
                    <HStack align="center">
                      <CopyButton
                        size="small"
                        copyText={utbetaling.journalpostId}
                        title="Kopier journalpost-ID"
                      />
                      {utbetaling.journalpostId}
                    </HStack>
                  }
                />
              )}

              {utbetaling.begrunnelseMindreBetalt && (
                <MetadataFritekstfelt
                  label={utbetalingTekster.metadata.begrunnelseMindreBetalt}
                  value={utbetaling.begrunnelseMindreBetalt}
                />
              )}
              {utbetaling.kommentar && (
                <MetadataFritekstfelt
                  label={utbetalingTekster.metadata.kommentar}
                  value={utbetaling.kommentar}
                />
              )}
            </VStack>
          </Box>
        </TwoColumnGrid>
        <EndringshistorikkPopover>
          <ViewEndringshistorikk historikk={historikk} />
        </EndringshistorikkPopover>
      </HGrid>
      <UtbetalingBeregningView utbetalingId={utbetaling.id} beregning={beregning} />
      <UtbetalingLinjeView utbetaling={utbetaling} handlinger={handlinger} />
    </VStack>
  );
}

interface UtbetalingLinjeViewProps {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
}

function UtbetalingLinjeView({ utbetaling, handlinger }: UtbetalingLinjeViewProps) {
  const { data: utbetalingLinjer } = useUtbetalingsLinjer(utbetaling.id);

  switch (utbetaling.status.type) {
    case UtbetalingStatusDtoType.VENTER_PA_ARRANGOR:
    case UtbetalingStatusDtoType.UBEHANDLET_FORSLAG:
    case UtbetalingStatusDtoType.AVBRUTT:
      return null;

    case UtbetalingStatusDtoType.RETURNERT:
    case UtbetalingStatusDtoType.KLAR_TIL_BEHANDLING:
      return (
        <RedigerUtbetalingLinjeView
          utbetaling={utbetaling}
          handlinger={handlinger}
          utbetalingLinjer={utbetalingLinjer}
        />
      );

    case UtbetalingStatusDtoType.TIL_ATTESTERING:
    case UtbetalingStatusDtoType.OVERFORT_TIL_UTBETALING:
    case UtbetalingStatusDtoType.DELVIS_UTBETALT:
    case UtbetalingStatusDtoType.UTBETALT:
      return <BesluttUtbetalingLinjeView utbetaling={utbetaling} handlinger={handlinger} />;
  }
}
