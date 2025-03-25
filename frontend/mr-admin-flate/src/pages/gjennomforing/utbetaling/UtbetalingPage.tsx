import { Header } from "@/components/detaljside/Header";
import { Metadata, MetadataHorisontal, Separator } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { formaterDato, formaterPeriode } from "@/utils/Utils";
import { AdminUtbetalingStatus, NavAnsattRolle } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteIcon } from "@navikt/aksel-icons";
import { Box, Heading, HStack, VStack } from "@navikt/ds-react";
import { useParams } from "react-router";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import {
  tilsagnTilUtbetalingQuery,
  utbetalingHistorikkQuery,
  utbetalingQuery,
} from "./utbetalingPageLoader";

import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { RedigerUtbetalingLinjeRows } from "@/components/utbetaling/RedigerUtbetalingLinjeRows";
import { UtbetalingLinjeRows } from "@/components/utbetaling/UtbetalingLinjeRows";

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
  };
}

export function UtbetalingPage() {
  const { gjennomforingId } = useParams();
  const { gjennomforing, ansatt, historikk, tilsagn, utbetaling, linjer } =
    useUtbetalingPageData();

  const erSaksbehandlerOkonomi = ansatt.roller.includes(
    NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
  );
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
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <BankNoteIcon className="w-10 h-10" />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Utbetaling for {gjennomforing.navn}
          </HStack>
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
                <HStack justify="space-between">
                  <VStack>
                    <Heading size="medium">Til utbetaling</Heading>
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
                  <VStack>
                    <Heading size="medium" className="mt-4">
                      Betalingsinformasjon
                    </Heading>
                    <VStack gap="2">
                      <Metadata
                        horizontal
                        header="Kontonummer"
                        verdi={utbetaling.betalingsinformasjon?.kontonummer}
                      />
                      <Metadata
                        horizontal
                        header="KID"
                        verdi={utbetaling.betalingsinformasjon?.kid || "-"}
                      />
                    </VStack>
                  </VStack>
                </HStack>
                <Separator />
                {erSaksbehandlerOkonomi &&
                [AdminUtbetalingStatus.BEHANDLES_AV_NAV, AdminUtbetalingStatus.RETURNERT].includes(
                  utbetaling.status,
                ) ? (
                  <RedigerUtbetalingLinjeRows
                    tilsagn={tilsagn}
                    utbetaling={utbetaling}
                    linjer={linjer}
                  />
                ) : (
                  <UtbetalingLinjeRows
                    utbetaling={utbetaling}
                    linjer={linjer}
                  />
                )}
              </VStack>
            </Box>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
