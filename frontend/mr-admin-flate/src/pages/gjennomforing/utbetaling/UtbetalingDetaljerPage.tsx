import { useEffect } from "react";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import {
  Betalingsinformasjon,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingStatusDtoType,
} from "@tiltaksadministrasjon/api-client";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import {
  Accordion,
  BodyShort,
  CopyButton,
  Heading,
  HGrid,
  HStack,
  InfoCard,
  Link,
  List,
  VStack,
} from "@navikt/ds-react";
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
import { QueryKeys } from "@/api/QueryKeys";
import { useQueryClient } from "@tanstack/react-query";
import {
  MetadataFritekstfelt,
  MetadataHGrid,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";

function useUtbetalingDetaljerData() {
  const { utbetalingId } = useRequiredParams(["utbetalingId"]);

  const { data: historikk } = useUtbetalingEndringshistorikk(utbetalingId);
  const { data: utbetalingDetaljer } = useUtbetaling(utbetalingId);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter: [] }, utbetalingId);

  // @todo: This is quickfix. Figure out why it scrolls to the bottom on page load as a part of the broader frontend improvements
  useEffect(() => {
    window.scrollTo(0, 0); // Reset scroll position to the top
  }, []);

  return {
    historikk,
    utbetaling: utbetalingDetaljer.utbetaling,
    handlinger: utbetalingDetaljer.handlinger,
    beregning,
  };
}

export function UtbetalingDetaljerPage() {
  const { historikk, utbetaling, handlinger, beregning } = useUtbetalingDetaljerData();

  return (
    <VStack
      id="kostnadsfordeling"
      gap="space-24"
      padding="space-16"
      className="rounded-lg border-ax-neutral-400 border"
    >
      <HGrid columns="1fr 1fr 0.25fr">
        <VStack>
          <Heading size="medium" level="2" spacing data-testid="utbetaling-til-utbetaling">
            {utbetalingTekster.metadata.header}
          </Heading>
          <VStack gap="space-4">
            <MetadataHGrid
              label={utbetalingTekster.metadata.status}
              value={<UtbetalingStatusTag status={utbetaling.status} />}
            />
            {utbetaling.avbruttBegrunnelse && (
              <MetadataFritekstfelt
                label={utbetalingTekster.metadata.avbruttBegrunnelse}
                value={utbetaling.avbruttBegrunnelse}
              />
            )}
            <MetadataHGrid
              label={utbetalingTekster.metadata.periode}
              value={formaterPeriode(utbetaling.periode)}
            />
            <MetadataHGrid
              label={utbetalingTekster.metadata.utbetalesTidligstDato}
              value={formaterDato(utbetaling.utbetalesTidligstDato)}
            />
            <MetadataHGrid
              label={utbetalingTekster.metadata.innsendtDato}
              value={formaterDato(utbetaling.innsendtAvArrangorDato)}
            />
            <MetadataHGrid
              label={utbetalingTekster.metadata.innsendtAv}
              value={utbetaling.innsendtAv}
            />
            <MetadataHGrid
              label={utbetalingTekster.beregning.belop.label}
              value={formaterValutaBelop(utbetaling.pris)}
            />
            {utbetaling.type.tagName && (
              <MetadataHGrid
                label={utbetalingTekster.metadata.type}
                value={
                  <HStack gap="space-4">
                    {utbetaling.type.displayName}
                    <UtbetalingTypeTag type={utbetaling.type.displayName} />
                  </HStack>
                }
              />
            )}
            {utbetaling.korreksjon?.opprinneligUtbetaling && (
              <MetadataHGrid
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
            {utbetaling.korreksjon && (
              <MetadataFritekstfelt
                label={utbetalingTekster.korreksjon.begrunnelse}
                value={utbetaling.korreksjon.begrunnelse}
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
        </VStack>
        <VStack gap="space-16">
          <Heading size="medium" level="2">
            Betalingsinformasjon
          </Heading>
          <VStack gap="space-16">
            {utbetaling.betalingsinformasjon && (
              <BetalingsinformasjonDetaljer
                betalingsinformasjon={utbetaling.betalingsinformasjon}
              />
            )}
          </VStack>
          {utbetaling.journalpostId ? (
            <>
              <Heading size="medium" level="2">
                Journalføring
              </Heading>
              <VStack gap="space-16">
                <MetadataHGrid
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
              </VStack>
            </>
          ) : null}
        </VStack>
        <HStack justify="end" align="start">
          <EndringshistorikkPopover>
            <ViewEndringshistorikk historikk={historikk} />
          </EndringshistorikkPopover>
        </HStack>
      </HGrid>
      {beregning.advarsler.length > 0 && (
        <InfoCard data-color="warning">
          <InfoCard.Header>
            <InfoCard.Title>Viktig informasjon om deltakere</InfoCard.Title>
          </InfoCard.Header>
          <InfoCard.Content>
            <BodyShort spacing>
              Det finnes advarsler på følgende personer. Disse må først fikses før utbetalingen kan
              sendes inn.
            </BodyShort>
            <List data-aksel-migrated-v8>
              {beregning.advarsler.map((advarsel) => (
                <List.Item key={advarsel.deltakerId}>{advarsel.beskrivelse}</List.Item>
              ))}
            </List>
          </InfoCard.Content>
        </InfoCard>
      )}
      <Accordion>
        <Accordion.Item>
          <Accordion.Header>Beregning - {beregning.heading}</Accordion.Header>
          <Accordion.Content>
            <UtbetalingBeregningView utbetalingId={utbetaling.id} beregning={beregning} />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
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
  const queryClient = useQueryClient();

  async function oppdaterLinjer() {
    await queryClient.refetchQueries({
      queryKey: QueryKeys.utbetaling(utbetaling.id),
    });
  }

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
          oppdaterLinjer={oppdaterLinjer}
        />
      );

    case UtbetalingStatusDtoType.TIL_ATTESTERING:
    case UtbetalingStatusDtoType.OVERFORT_TIL_UTBETALING:
    case UtbetalingStatusDtoType.DELVIS_UTBETALT:
    case UtbetalingStatusDtoType.UTBETALT:
      return (
        <BesluttUtbetalingLinjeView
          utbetaling={utbetaling}
          handlinger={handlinger}
          oppdaterLinjer={oppdaterLinjer}
        />
      );
  }
}

function BetalingsinformasjonDetaljer({
  betalingsinformasjon,
}: {
  betalingsinformasjon: Betalingsinformasjon;
}) {
  switch (betalingsinformasjon.type) {
    case "BBan":
      return (
        <VStack gap="space-8">
          <MetadataHGrid label="Kontonummer" value={betalingsinformasjon.kontonummer} />
          <MetadataHGrid label="KID (valgfritt)" value={betalingsinformasjon.kid} />
        </VStack>
      );
    case "IBan":
      return (
        <VStack gap="space-8">
          <MetadataHGrid label="IBan" value={betalingsinformasjon.iban} />
          <MetadataHGrid label="BIC/SWIFT" value={betalingsinformasjon.bic} />
          <MetadataHGrid label="Banknavn" value={betalingsinformasjon.bankNavn} />
          <MetadataHGrid label="Bank landkode" value={betalingsinformasjon.bankLandKode} />
        </VStack>
      );
    case undefined:
      throw Error("unreachable");
  }
}
