import { useEffect, useState } from "react";
import { MetadataFritekstfelt, MetadataHorisontal } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import {
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingStatusDtoType,
} from "@tiltaksadministrasjon/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteFillIcon } from "@navikt/aksel-icons";
import { Accordion, CopyButton, Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";

import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { UtbetalingTypeText } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
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

function useUtbetalingPageData() {
  const { gjennomforingId, utbetalingId } = useRequiredParams(["gjennomforingId", "utbetalingId"]);

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: historikk } = useUtbetalingEndringshistorikk(utbetalingId);
  const { data: utbetalingDetaljer } = useUtbetaling(utbetalingId);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter: [] }, utbetalingId);

  // @todo: This is quickfix. Figure out why it scrolls to the bottom on page load as a part of the broader frontend improvements
  useEffect(() => {
    window.scrollTo(0, 0); // Reset scroll position to the top
  }, []);

  return {
    gjennomforing,
    historikk,
    utbetaling: utbetalingDetaljer.utbetaling,
    handlinger: utbetalingDetaljer.handlinger,
    beregning,
  };
}

export function UtbetalingPage() {
  const { gjennomforing, historikk, utbetaling, handlinger, beregning } = useUtbetalingPageData();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforing.id}/utbetalinger`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <title>{utbetalingTekster.title}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="2" className="bg-white border-b-2 border-gray-200 p-2">
        <BankNoteFillIcon color="#2AA758" className="w-10 h-10" />
        <Heading size="large" level="1">
          {utbetalingTekster.header(gjennomforing.navn)}
        </Heading>
      </HStack>
      <ContentBox>
        <WhitePaddedBox>
          <VStack gap="4">
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
            <VStack
              gap="4"
              id="kostnadsfordeling"
              padding="4"
              className="border-gray-300 border-1 rounded-lg"
            >
              <HGrid columns="1fr 1fr 0.25fr">
                <VStack>
                  <Heading size="medium" level="2" spacing data-testid="utbetaling-til-utbetaling">
                    {utbetalingTekster.metadata.header}
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header={utbetalingTekster.metadata.status}
                      value={<UtbetalingStatusTag status={utbetaling.status} />}
                    />

                    <MetadataHorisontal
                      header={utbetalingTekster.metadata.periode}
                      value={formaterPeriode(utbetaling.periode)}
                    />
                    {utbetaling.type.tagName && (
                      <MetadataHorisontal
                        header={utbetalingTekster.metadata.type}
                        value={<UtbetalingTypeText type={utbetaling.type} />}
                      />
                    )}
                    <MetadataHorisontal
                      header={utbetalingTekster.metadata.innsendtDato}
                      value={formaterDato(utbetaling.godkjentAvArrangorTidspunkt)}
                    />
                    <MetadataHorisontal
                      header={utbetalingTekster.metadata.innsendtAv}
                      value={utbetaling.innsendtAv}
                    />
                    <MetadataHorisontal
                      header={utbetalingTekster.beregning.belop.label}
                      value={formaterNOK(utbetaling.belop)}
                    />
                    {utbetaling.beskrivelse && (
                      <MetadataFritekstfelt
                        header={utbetalingTekster.metadata.beskrivelse}
                        value={utbetaling.beskrivelse}
                      />
                    )}
                    {utbetaling.begrunnelseMindreBetalt && (
                      <MetadataFritekstfelt
                        header={utbetalingTekster.metadata.begrunnelseMindreBetalt}
                        value={utbetaling.begrunnelseMindreBetalt}
                      />
                    )}
                  </VStack>
                </VStack>
                <VStack gap="4">
                  <Heading size="medium" level="2">
                    Betalingsinformasjon
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Kontonummer"
                      value={utbetaling.betalingsinformasjon.kontonummer}
                    />
                    <MetadataHorisontal
                      header="KID (valgfritt)"
                      value={utbetaling.betalingsinformasjon.kid}
                    />
                  </VStack>
                  {utbetaling.journalpostId ? (
                    <>
                      <Heading size="medium" level="2">
                        Journalføring
                      </Heading>
                      <VStack gap="2">
                        <MetadataHorisontal
                          header="Journalpost-ID i Gosys"
                          value={
                            <HStack align="center">
                              <CopyButton
                                size="small"
                                variant="action"
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
              <Accordion>
                <Accordion.Item>
                  <Accordion.Header>Beregning - {beregning.heading}</Accordion.Header>
                  <Accordion.Content>
                    {utbetaling.id && (
                      <UtbetalingBeregningView utbetalingId={utbetaling.id} beregning={beregning} />
                    )}
                  </Accordion.Content>
                </Accordion.Item>
              </Accordion>
              <UtbetalingLinjeView utbetaling={utbetaling} handlinger={handlinger} />
            </VStack>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}

interface UtbetalingLinjeViewProps {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
}

function UtbetalingLinjeView({ utbetaling, handlinger }: UtbetalingLinjeViewProps) {
  const [reloadLinjer, setReloadLinjer] = useState<boolean>();
  const { data: utbetalingLinjer, isRefetching } = useUtbetalingsLinjer(utbetaling.id);
  const queryClient = useQueryClient();

  const linjerIsUpdated = !isRefetching && reloadLinjer;

  useEffect(() => {
    if (linjerIsUpdated) {
      setReloadLinjer(false);
    }
  }, [linjerIsUpdated, setReloadLinjer]);

  async function oppdaterLinjer() {
    await queryClient.refetchQueries({
      queryKey: QueryKeys.utbetaling(utbetaling.id),
    });
    setReloadLinjer(true);
  }

  switch (utbetaling.status.type) {
    case UtbetalingStatusDtoType.VENTER_PA_ARRANGOR:
      return null;
    case UtbetalingStatusDtoType.RETURNERT:
    case UtbetalingStatusDtoType.KLAR_TIL_BEHANDLING:
      return (
        <RedigerUtbetalingLinjeView
          utbetaling={utbetaling}
          handlinger={handlinger}
          utbetalingLinjer={utbetalingLinjer}
          oppdaterLinjer={oppdaterLinjer}
          reloadLinjer={linjerIsUpdated}
        />
      );
    case UtbetalingStatusDtoType.TIL_ATTESTERING:
    case UtbetalingStatusDtoType.OVERFORT_TIL_UTBETALING:
      return <BesluttUtbetalingLinjeView utbetaling={utbetaling} oppdaterLinjer={oppdaterLinjer} />;
  }
}
