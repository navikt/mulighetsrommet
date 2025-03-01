import { Header } from "@/components/detaljside/Header";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { OpprettTilsagnButton } from "@/components/tilsagn/OpprettTilsagnButton";
import { DelutbetalingRow } from "@/components/utbetaling/DelutbetalingRow";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { LoaderData } from "@/types/loader";
import { formaterDato } from "@/utils/Utils";
import {
  NavAnsattRolle,
  Prismodell,
  TilsagnDefaultsRequest,
  TilsagnDto,
  TilsagnType,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteIcon } from "@navikt/aksel-icons";
import { Alert, Box, CopyButton, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useLoaderData } from "react-router";
import { utbetalingPageLoader } from "./utbetalingPageLoader";
export function UtbetalingPage() {
  const { gjennomforing, historikk, utbetaling, tilsagn, ansatt } =
    useLoaderData<LoaderData<typeof utbetalingPageLoader>>();
  const [belopPerTilsagn, setBelopPerTilsagn] = useState<Map<string, number>>(
    new Map(
      tilsagn.map((tilsagn) => [
        tilsagn.id,
        utbetaling.delutbetalinger.find((d) => d.tilsagnId === tilsagn.id)?.belop ?? 0,
      ]),
    ),
  );

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
    return [...belopPerTilsagn.values()].reduce((acc, val) => acc + val, 0);
  }

  function differanse(): number {
    return utbetaling.beregning.belop - utbetalesTotal();
  }

  function ekstraTilsagnDefaults(): TilsagnDefaultsRequest {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    const defaultBelop =
      tilsagn.length === 0
        ? utbetaling.beregning.belop
        : defaultTilsagn
          ? utbetaling.beregning.belop - (belopPerTilsagn.get(defaultTilsagn.id) ?? 0)
          : 0;
    return {
      gjennomforingId: gjennomforing.id,
      type: TilsagnType.EKSTRATILSAGN,
      prismodell: Prismodell.FRI,
      belop: defaultBelop,
      periodeStart: utbetaling.beregning.periodeStart,
      periodeSlutt: utbetaling.beregning.periodeSlutt,
      kostnadssted: defaultTilsagn?.kostnadssted.enhetsnummer,
    };
  }

  const skriveTilgang = ansatt?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV);

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
                <Heading size="medium">Til utbetaling</Heading>
                <VStack gap="2">
                  <Metadata
                    horizontal
                    header="Utbetalingsperiode"
                    verdi={`${formaterDato(utbetaling.beregning.periodeStart)} - ${formaterDato(utbetaling.beregning.periodeSlutt)}`}
                  />
                  <Metadata
                    horizontal
                    header="Innsendt"
                    verdi={formaterDato(
                      utbetaling.godkjentAvArrangorTidspunkt ?? utbetaling.createdAt,
                    )}
                  />
                  <Metadata
                    horizontal
                    header="Beløp til utbetaling"
                    verdi={formaterNOK(utbetaling.beregning.belop)}
                  />
                </VStack>
                <Separator />
                <HStack justify="space-between">
                  <Heading size="medium">Tilsagn</Heading>
                  {skriveTilgang && <OpprettTilsagnButton defaults={ekstraTilsagnDefaults()} />}
                </HStack>
                {tilsagn.length === 0 && <Alert variant="info">Tilsagn mangler</Alert>}
                {tilsagn.length > 0 && (
                  <Table>
                    <Table.Header>
                      <Table.Row>
                        <Table.HeaderCell></Table.HeaderCell>
                        <Table.HeaderCell>Periodestart</Table.HeaderCell>
                        <Table.HeaderCell>Periodeslutt</Table.HeaderCell>
                        <Table.HeaderCell>Type</Table.HeaderCell>
                        <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
                        <Table.HeaderCell>Gjenstående beløp</Table.HeaderCell>
                        <Table.HeaderCell>Utbetales</Table.HeaderCell>
                        <Table.HeaderCell>Status</Table.HeaderCell>
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
                            onBelopChange={(belop) =>
                              setBelopPerTilsagn((prevMap) => {
                                const newMap = new Map(prevMap);
                                newMap.set(t.id, belop);
                                return newMap;
                              })
                            }
                          />
                        );
                      })}
                      <Table.Row>
                        <Table.DataCell
                          colSpan={2}
                          className="font-bold"
                        >{`Beløp til utbetaling ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
                        <Table.DataCell>-</Table.DataCell>
                        <Table.DataCell>-</Table.DataCell>
                        <Table.DataCell>-</Table.DataCell>
                        <Table.DataCell>-</Table.DataCell>
                        <Table.DataCell className="font-bold">
                          {formaterNOK(utbetalesTotal())}
                        </Table.DataCell>
                        <Table.DataCell>
                          <CopyButton
                            copyText={String(differanse())}
                            text={`Differanse ${formaterNOK(differanse())}`}
                            activeText={`Differanse ${formaterNOK(differanse())}`}
                          />
                        </Table.DataCell>
                      </Table.Row>
                    </Table.Body>
                  </Table>
                )}
                <Separator />
                <Heading size="medium">Betalingsinformasjon</Heading>
                <VStack gap="2">
                  <Metadata
                    horizontal
                    header="Kontonummer"
                    verdi={utbetaling.betalingsinformasjon?.kontonummer}
                  />
                  <Metadata
                    horizontal
                    header="KID"
                    verdi={utbetaling.betalingsinformasjon?.kid ?? "N/A"}
                  />
                </VStack>
              </VStack>
            </Box>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
