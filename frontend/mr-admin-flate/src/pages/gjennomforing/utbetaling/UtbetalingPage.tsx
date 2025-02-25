import { Header } from "@/components/detaljside/Header";
import { Metadata, MetadataHorisontal, Separator } from "@/components/detaljside/Metadata";
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
  TilsagnStatus,
  TilsagnType,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteIcon, PencilFillIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { ActionMenu, Alert, Box, Button, Heading, HStack, Table, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useLoaderData, useNavigate } from "react-router";
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
  const avvistUtbetaling = utbetaling.delutbetalinger.find(
    (d) => d.type === "DELUTBETALING_AVVIST",
  );
  const navigate = useNavigate();
  const [endreUtbetaling, setEndreUtbetaling] = useState<boolean>(!avvistUtbetaling);

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

  function totalGjenståendeBeløp(): number {
    return tilsagn
      .map((tilsagn) =>
        tilsagn.status.type === TilsagnStatus.GODKJENT ? tilsagn.beregning.output.belop : 0,
      )
      .reduce((acc, val) => acc + val, 0);
  }

  function differanse(): number {
    return totalGjenståendeBeløp() - utbetalesTotal();
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

  function opprettTilsagn() {
    navigate(
      `/gjennomforinger/${ekstraTilsagnDefaults().gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${ekstraTilsagnDefaults().type}` +
        `&prismodell=${ekstraTilsagnDefaults().prismodell}` +
        `&belop=${ekstraTilsagnDefaults().belop}` +
        `&periodeStart=${ekstraTilsagnDefaults().periodeStart}` +
        `&periodeSlutt=${ekstraTilsagnDefaults().periodeSlutt}` +
        `&kostnadssted=${ekstraTilsagnDefaults().kostnadssted}`,
    );
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
                  <MetadataHorisontal
                    header="Utbetalingsperiode"
                    verdi={`${formaterDato(utbetaling.beregning.periodeStart)} - ${formaterDato(utbetaling.beregning.periodeSlutt)}`}
                  />
                  <MetadataHorisontal
                    header="Innsendt"
                    verdi={formaterDato(
                      utbetaling.godkjentAvArrangorTidspunkt ?? utbetaling.createdAt,
                    )}
                  />
                  <MetadataHorisontal
                    header="Beløp arrangør har sendt inn"
                    verdi={formaterNOK(utbetaling.beregning.belop)}
                  />
                </VStack>
                <Separator />
                <HStack justify="space-between">
                  <Heading size="medium">Tilsagn</Heading>
                  {skriveTilgang &&
                    (avvistUtbetaling ? (
                      <ActionMenu>
                        <ActionMenu.Trigger>
                          <Button variant="primary" size="small">
                            Handlinger
                          </Button>
                        </ActionMenu.Trigger>
                        <ActionMenu.Content>
                          <ActionMenu.Item
                            icon={<PiggybankIcon />}
                            onSelect={() => opprettTilsagn()}
                          >
                            Opprett tilsagn
                          </ActionMenu.Item>
                          <ActionMenu.Item
                            icon={<PencilFillIcon />}
                            onSelect={() => setEndreUtbetaling(true)}
                          >
                            Endre utbetaling
                          </ActionMenu.Item>
                        </ActionMenu.Content>
                      </ActionMenu>
                    ) : (
                      <OpprettTilsagnButton defaults={ekstraTilsagnDefaults()} />
                    ))}
                </HStack>
                {tilsagn.length === 0 && <Alert variant="info">Tilsagn mangler</Alert>}
                {tilsagn.length > 0 && (
                  <Table>
                    <Table.Header>
                      <Table.Row>
                        <Table.HeaderCell />
                        <Table.HeaderCell scope="col">Periodestart</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Periodeslutt</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Kostnadssted</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Tilgjengelig på tilsagn</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Utbetales</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Status</Table.HeaderCell>
                        <Table.HeaderCell />
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
                            endreUtbetaling={endreUtbetaling}
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
                          colSpan={5}
                          className="font-bold"
                        >{`Beløp arrangør har sendt inn ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
                        <Table.DataCell className="font-bold">
                          {formaterNOK(totalGjenståendeBeløp())}
                        </Table.DataCell>
                        <Table.DataCell className="font-bold">
                          {formaterNOK(utbetalesTotal())}
                        </Table.DataCell>
                        <Table.DataCell colSpan={2} className="font-bold">
                          {`Differanse ${formaterNOK(differanse())}`}
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
