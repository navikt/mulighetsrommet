import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { utbetalingLinjeCompareFn } from "@/utils/Utils";
import { Rolle, TilsagnStatus } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteFillIcon } from "@navikt/aksel-icons";
import { Accordion, Alert, CopyButton, Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { useParams } from "react-router";
import {
  beregningQuery,
  tilsagnTilUtbetalingQuery,
  utbetalingHistorikkQuery,
  utbetalingQuery,
} from "./utbetalingPageLoader";

import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { BesluttUtbetalingLinjeView } from "@/components/utbetaling/BesluttUtbetalingLinjeView";
import { RedigerUtbetalingLinjeView } from "@/components/utbetaling/RedigerUtbetalingLinjeView";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useEffect } from "react";
import { UtbetalingTypeText } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import UtbetalingBeregningView from "@/components/utbetaling/beregning/UtbetalingBeregningView";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { AarsakerOgForklaring } from "../tilsagn/AarsakerOgForklaring";

function useUtbetalingPageData() {
  const { gjennomforingId, utbetalingId } = useParams();

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: ansatt } = useHentAnsatt();
  const { data: historikk } = useApiSuspenseQuery(utbetalingHistorikkQuery(utbetalingId));
  const { data: utbetaling } = useApiSuspenseQuery(utbetalingQuery(utbetalingId));
  const { data: tilsagn } = useApiSuspenseQuery(tilsagnTilUtbetalingQuery(utbetalingId));
  const { data: beregning } = useApiSuspenseQuery(beregningQuery({ navEnheter: [] }, utbetalingId));

  // @todo: This is quickfix. Figure out why it scrolls to the bottom on page load as a part of the broader frontend improvements
  useEffect(() => {
    window.scrollTo(0, 0); // Reset scroll position to the top
  }, []);

  return {
    gjennomforing,
    ansatt,
    historikk,
    tilsagn,
    utbetaling: utbetaling.utbetaling,
    linjer: utbetaling.linjer.toSorted(utbetalingLinjeCompareFn),
    beregning,
  };
}

export function UtbetalingPage() {
  const { gjennomforingId, utbetalingId } = useParams();
  const { gjennomforing, ansatt, historikk, tilsagn, utbetaling, linjer, beregning } =
    useUtbetalingPageData();

  const erSaksbehandlerOkonomi = ansatt.roller.includes(Rolle.SAKSBEHANDLER_OKONOMI);
  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforingId}/utbetalinger`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <title>Utbetalinger</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="2" className="bg-white border-b-2 border-gray-200 p-2">
        <BankNoteFillIcon color="#2AA758" className="w-10 h-10" />
        <Heading size="large" level="1">
          Utbetaling for {gjennomforing.navn}
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
              {utbetaling.status.type === "AVBRUTT" && (
                <AarsakerOgForklaring
                  heading="Utbetaling avbrutt"
                  tekster={[`Tidspunkt ${formaterDato(utbetaling.status.tidspunkt)}`]}
                  aarsaker={utbetaling.status.aarsaker}
                  forklaring={utbetaling.status.forklaring}
                />
              )}
              <HGrid columns="1fr 1fr 0.25fr">
                <VStack>
                  <Heading size="medium" level="2" spacing data-testid="utbetaling-til-utbetaling">
                    Til utbetaling
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Status"
                      verdi={<UtbetalingStatusTag status={utbetaling.status} />}
                    />

                    <MetadataHorisontal
                      header="Utbetalingsperiode"
                      verdi={formaterPeriode(utbetaling.periode)}
                    />
                    {utbetaling.type && (
                      <MetadataHorisontal
                        header="Type"
                        verdi={<UtbetalingTypeText type={utbetaling.type} />}
                      />
                    )}
                    <MetadataHorisontal
                      header="Dato innsendt"
                      verdi={formaterDato(
                        utbetaling.godkjentAvArrangorTidspunkt ?? utbetaling.createdAt,
                      )}
                    />
                    <MetadataHorisontal
                      header="Innsendt av"
                      verdi={utbetaling.innsendtAv || "Ukjent innsender"}
                    />
                    <MetadataHorisontal
                      header={utbetalingTekster.beregning.belop.label}
                      verdi={formaterNOK(utbetaling.belop)}
                    />
                    {utbetaling.beskrivelse && (
                      <MetadataHorisontal
                        header="Begrunnelse for utbetaling"
                        verdi={utbetaling.beskrivelse}
                      />
                    )}
                    {utbetaling.begrunnelseMindreBetalt && (
                      <MetadataHorisontal
                        header="Begrunnelse for mindre betalt"
                        verdi={utbetaling.begrunnelseMindreBetalt}
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
                      verdi={utbetaling.betalingsinformasjon?.kontonummer}
                    />
                    <MetadataHorisontal
                      header="KID (valgfritt)"
                      verdi={utbetaling.betalingsinformasjon?.kid || "-"}
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
                          verdi={
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
              {utbetaling.status.type != "AVBRUTT" && (
                <>
                  <Accordion>
                    <Accordion.Item>
                      <Accordion.Header>Beregning - {beregning.heading}</Accordion.Header>
                      <Accordion.Content>
                        {utbetalingId && (
                          <UtbetalingBeregningView
                            utbetalingId={utbetalingId}
                            beregning={beregning}
                          />
                        )}
                      </Accordion.Content>
                    </Accordion.Item>
                  </Accordion>
                  {tilsagn.every(
                    (t) => ![TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT].includes(t.status),
                  ) && (
                    <Alert variant="info">
                      Det finnes ingen godkjente tilsagn for utbetalingsperioden
                    </Alert>
                  )}
                  {erSaksbehandlerOkonomi &&
                  ["KLAR_TIL_BEHANDLING", "RETURNERT"].includes(utbetaling.status.type) ? (
                    <RedigerUtbetalingLinjeView
                      tilsagn={tilsagn}
                      utbetaling={utbetaling}
                      linjer={linjer}
                    />
                  ) : (
                    <BesluttUtbetalingLinjeView utbetaling={utbetaling} linjer={linjer} />
                  )}
                </>
              )}
            </VStack>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
