import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { Header } from "@/components/detaljside/Header";
import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { formaterDato, formaterPeriode } from "@/utils/Utils";
import { AdminUtbetalingStatus, Rolle, TilsagnStatus } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteIcon } from "@navikt/aksel-icons";
import {
  Accordion,
  Alert,
  Box,
  CopyButton,
  Heading,
  HGrid,
  HStack,
  VStack,
} from "@navikt/ds-react";
import { useParams } from "react-router";
import {
  tilsagnTilUtbetalingQuery,
  utbetalingHistorikkQuery,
  utbetalingQuery,
} from "./utbetalingPageLoader";

import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { BesluttUtbetalingLinjeView } from "@/components/utbetaling/BesluttUtbetalingLinjeView";
import { Deltakeroversikt } from "@/components/utbetaling/Deltakeroversikt";
import { RedigerUtbetalingLinjeView } from "@/components/utbetaling/RedigerUtbetalingLinjeView";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useApiSuspenseQuery } from "@mr/frontend-common";

function useUtbetalingPageData() {
  const { gjennomforingId, utbetalingId } = useParams();

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: ansatt } = useHentAnsatt();
  const { data: historikk } = useApiSuspenseQuery(utbetalingHistorikkQuery(utbetalingId));
  const { data: utbetaling } = useApiSuspenseQuery(utbetalingQuery(utbetalingId));
  const { data: tilsagn } = useApiSuspenseQuery(tilsagnTilUtbetalingQuery(utbetalingId));

  return {
    gjennomforing,
    ansatt,
    historikk,
    tilsagn,
    utbetaling: utbetaling.utbetaling,
    linjer: utbetaling.linjer.toSorted((m, n) => m.id.localeCompare(n.id)),
    deltakere: utbetaling.deltakere,
  };
}

export function UtbetalingPage() {
  const { gjennomforingId } = useParams();
  const { gjennomforing, ansatt, historikk, tilsagn, utbetaling, linjer, deltakere } =
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
      <Header>
        <BankNoteIcon className="w-10 h-10" />
        <Heading size="large" level="1">
          Utbetaling for {gjennomforing.navn}
        </Heading>
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <HStack gap="2" justify={"end"}>
            <EndringshistorikkPopover>
              <ViewEndringshistorikk historikk={historikk} />
            </EndringshistorikkPopover>
          </HStack>
          <VStack gap="4">
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
            <Box borderColor="border-subtle" padding="4" borderWidth="1" borderRadius="large">
              <VStack gap="4" id="kostnadsfordeling">
                <VStack>
                  <div className="self-end">
                    <UtbetalingStatusTag status={utbetaling.status} />
                  </div>
                  <HGrid columns="1fr 1fr">
                    <VStack>
                      <Heading
                        size="medium"
                        level="2"
                        spacing
                        data-testid="utbetaling-til-utbetaling"
                      >
                        Til utbetaling
                      </Heading>
                      <VStack gap="2">
                        <MetadataHorisontal
                          header="Utbetalingsperiode"
                          verdi={formaterPeriode(utbetaling.periode)}
                        />
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
                          verdi={formaterNOK(utbetaling.beregning.belop)}
                        />
                        {utbetaling.beskrivelse && (
                          <MetadataHorisontal
                            header="Begrunnelse for utbetaling"
                            verdi={utbetaling.beskrivelse}
                          />
                        )}
                      </VStack>
                    </VStack>
                    <VStack gap="4">
                      <>
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
                            verdi={utbetaling.betalingsinformasjon?.kid || "Ikke oppgitt"}
                          />
                        </VStack>
                      </>
                      {utbetaling.journalpostId ? (
                        <>
                          <Heading size="medium" level="2">
                            Journalføring
                          </Heading>
                          <VStack gap="2">
                            <MetadataHorisontal
                              header="Journalpost-ID i Gosys"
                              verdi={
                                <HStack align="center" gap="1">
                                  <CopyButton
                                    size="small"
                                    copyText={utbetaling.journalpostId}
                                    title="Kopier journalpost-ID"
                                  ></CopyButton>
                                  {utbetaling.journalpostId}
                                </HStack>
                              }
                            />
                          </VStack>
                        </>
                      ) : null}
                    </VStack>
                  </HGrid>
                </VStack>
                {deltakere.length > 0 && (
                  <Accordion>
                    <Accordion.Item>
                      <Accordion.Header>Deltakeroversikt</Accordion.Header>
                      <Accordion.Content>
                        <Deltakeroversikt deltakere={deltakere} />
                      </Accordion.Content>
                    </Accordion.Item>
                  </Accordion>
                )}
                {tilsagn.every(
                  (t) => ![TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT].includes(t.status),
                ) && (
                  <Alert variant="info">
                    Det finnes ingen godkjente tilsagn for utbetalingsperioden
                  </Alert>
                )}
                {erSaksbehandlerOkonomi &&
                [
                  AdminUtbetalingStatus.KLAR_TIL_BEHANDLING,
                  AdminUtbetalingStatus.RETURNERT,
                ].includes(utbetaling.status) ? (
                  <RedigerUtbetalingLinjeView
                    tilsagn={tilsagn}
                    utbetaling={utbetaling}
                    linjer={linjer}
                  />
                ) : (
                  <BesluttUtbetalingLinjeView utbetaling={utbetaling} linjer={linjer} />
                )}
              </VStack>
            </Box>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
