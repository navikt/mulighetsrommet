import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { TilsagnDto, TilsagnDefaultsRequest, TilsagnType, Prismodell } from "@mr/api-client-v2";
import { Alert, BodyShort, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { formaterDato } from "@/utils/Utils";
import { OpprettTilsagnLink } from "@/components/tilsagn/OpprettTilsagnLink";
import { DelutbetalingRow } from "@/components/utbetaling/DelutbetalingRow";
import { utbetalingPageLoader } from "./utbetalingPageLoader";

export function UtbetalingPage() {
  const { gjennomforing, utbetaling, tilsagn, ansatt } =
    useLoaderData<typeof utbetalingPageLoader>();

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

  function utbetalesTotal(): number {
    // TODO:
    return 0;
  }

  function ekstraTilsagnDefaults(): TilsagnDefaultsRequest {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    return {
      gjennomforingId: gjennomforing.id,
      type: TilsagnType.EKSTRATILSAGN,
      prismodell: Prismodell.FRI,
      belop: defaultTilsagn
        ? utbetaling.beregning.belop - defaultTilsagn.beregning.output.belop
        : null,
      periodeStart: utbetaling.beregning.periodeStart,
      periodeSlutt: utbetaling.beregning.periodeSlutt,
      kostnadssted: defaultTilsagn?.kostnadssted.enhetsnummer,
    };
  }

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Utbetaling for {gjennomforing.navn}
          </HStack>
        </Heading>
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <VStack gap="4">
            <VStack gap="4" id="kostnadsfordeling">
              <VStack gap="2">
                <HStack gap="4" align="center">
                  <Heading size="small">Periode:</Heading>
                  <BodyShort>
                    {formaterDato(utbetaling.beregning.periodeStart)} -{" "}
                    {formaterDato(utbetaling.beregning.periodeSlutt)}
                  </BodyShort>
                </HStack>
                <OpprettTilsagnLink defaults={ekstraTilsagnDefaults()} />
              </VStack>
              {tilsagn.length === 0 && <Alert variant="info">Tilsagn mangler</Alert>}
              {tilsagn.length > 0 && (
                <Table>
                  <Table.Header>
                    <Table.Row>
                      <Table.HeaderCell></Table.HeaderCell>
                      <Table.HeaderCell>Status</Table.HeaderCell>
                      <Table.HeaderCell>Periodestart</Table.HeaderCell>
                      <Table.HeaderCell>Periodeslutt</Table.HeaderCell>
                      <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
                      <Table.HeaderCell>Gjenstående beløp</Table.HeaderCell>
                      <Table.HeaderCell>Utbetales</Table.HeaderCell>
                      <Table.HeaderCell></Table.HeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {tilsagn.map((t: TilsagnDto) => {
                      return (
                        <DelutbetalingRow
                          key={t.id}
                          utbetaling={utbetaling}
                          tilsagn={t}
                          delutbetaling={utbetaling.delutbetalinger.find(
                            (d) => d.tilsagnId === t.id,
                          )}
                          ansatt={ansatt}
                        />
                      );
                    })}
                    <Table.Row>
                      <Table.DataCell className="font-bold">{`Opprinnelig krav ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
                      <Table.DataCell>-</Table.DataCell>
                      <Table.DataCell>-</Table.DataCell>
                      <Table.DataCell>-</Table.DataCell>
                      <Table.DataCell>-</Table.DataCell>
                      <Table.DataCell className="font-bold">
                        {formaterNOK(utbetalesTotal())}
                      </Table.DataCell>
                      <Table.DataCell></Table.DataCell>
                    </Table.Row>
                  </Table.Body>
                </Table>
              )}
            </VStack>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
