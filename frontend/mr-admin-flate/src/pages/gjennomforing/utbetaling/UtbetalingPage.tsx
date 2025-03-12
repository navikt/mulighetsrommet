import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { Header } from "@/components/detaljside/Header";
import { Metadata, MetadataHorisontal, Separator } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DelutbetalingRow } from "@/components/utbetaling/DelutbetalingRow";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { formaterDato } from "@/utils/Utils";
import {
  FieldError,
  NavAnsattRolle,
  OpprettDelutbetalingerRequest,
  Prismodell,
  TilsagnStatus,
  TilsagnType,
} from "@mr/api-client-v2";
import { formaterNOK, isValidationError } from "@mr/frontend-common/utils/utils";
import { BankNoteIcon, PencilFillIcon, PiggybankIcon } from "@navikt/aksel-icons";
import {
  ActionMenu,
  Alert,
  BodyShort,
  Box,
  Button,
  CopyButton,
  Heading,
  HStack,
  Table,
  VStack,
} from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams, useRevalidator } from "react-router";
import { OpprettDelutbetalingRow } from "@/components/utbetaling/OpprettDelutbetalingRow";
import { v4 as uuidv4 } from "uuid";
import { useHentAnsatt } from "../../../api/ansatt/useHentAnsatt";
import {
  delutbetalingerQuery,
  tilsagnTilUtbetalingQuery,
  utbetalingHistorikkQuery,
  utbetalingQuery,
} from "./utbetalingPageLoader";

import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useAdminGjennomforingById } from "../../../api/gjennomforing/useAdminGjennomforingById";
function useUtbetalingPageData() {
  const { gjennomforingId, utbetalingId } = useParams();

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: ansatt } = useHentAnsatt();
  const { data: historikk } = useApiSuspenseQuery(utbetalingHistorikkQuery(utbetalingId));
  const { data: utbetaling } = useApiSuspenseQuery(utbetalingQuery(utbetalingId));
  const { data: delutbetalinger } = useApiSuspenseQuery(delutbetalingerQuery(utbetalingId));
  const { data: tilsagn } = useApiSuspenseQuery(tilsagnTilUtbetalingQuery(utbetalingId));

  return {
    gjennomforing,
    ansatt,
    historikk,
    utbetaling,
    delutbetalinger,
    tilsagn,
  };
}

export function UtbetalingPage() {
  const { gjennomforingId } = useParams();
  const { gjennomforing, ansatt, historikk, utbetaling, delutbetalinger, tilsagn } =
    useUtbetalingPageData();

  const [delutbetalingPerTilsagn, setDelutbetalingPerTilsagn] = useState<
    { id?: string; tilsagnId: string; belop: number; frigjorTilsagn: boolean }[]
  >(
    delutbetalinger.map((d) => {
      return {
        id: d.id,
        tilsagnId: d.tilsagnId,
        belop: d.belop,
        frigjorTilsagn: d.frigjorTilsagn,
      };
    }),
  );

  const [endreUtbetaling, setEndreUtbetaling] = useState<boolean>(delutbetalinger.length === 0);
  const [error, setError] = useState<string | undefined>(undefined);

  const revalidator = useRevalidator();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  const skriveTilgang = ansatt?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV);
  const avvistUtbetaling = delutbetalinger.find((d) => d.type === "DELUTBETALING_AVVIST");
  const kanRedigeres: boolean =
    skriveTilgang && tilsagn.some((t) => t.status === TilsagnStatus.GODKJENT) && endreUtbetaling;

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

  const utbetalesTotal = delutbetalingPerTilsagn.reduce((acc, d) => acc + d.belop, 0);

  const totalGjenstaendeBelop = tilsagn
    .filter((tilsagn) => tilsagn.status === TilsagnStatus.GODKJENT)
    .reduce((acc, t) => acc + t.beregning.output.belop, 0);

  const differanse = utbetaling.beregning.belop - utbetalesTotal;

  function ekstraTilsagnDefaults() {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    const defaultBelop =
      tilsagn.length === 0
        ? utbetaling.beregning.belop
        : defaultTilsagn
          ? utbetaling.beregning.belop -
            (delutbetalingPerTilsagn.find((d) => d.tilsagnId === defaultTilsagn.id)?.belop ?? 0)
          : 0;
    return navigate(
      `/gjennomforinger/${gjennomforing.id}/tilsagn/opprett-tilsagn` +
        `?type=${TilsagnType.EKSTRATILSAGN}` +
        `&prismodell=${Prismodell.FRI}` +
        `&belop=${defaultBelop}` +
        `&periodeStart=${utbetaling.beregning.periodeStart}` +
        `&periodeSlutt=${utbetaling.beregning.periodeSlutt}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer}`,
    );
  }
  function sendTilGodkjenning() {
    const delutbetalingerr = delutbetalingPerTilsagn.map((d) => {
      return { ...d, id: d.id ?? uuidv4() };
    });
    if (utbetalesTotal <= 0) setError("Samlet beløp må være positivt");
    else {
      const body: OpprettDelutbetalingerRequest = {
        utbetalingId: utbetaling.id,
        delutbetalinger: delutbetalingerr,
      };

      opprettMutation.mutate(body, {
        onSuccess: async () => {
          setError(undefined);
          setEndreUtbetaling(false);
          await queryClient.invalidateQueries({
            queryKey: ["utbetaling", utbetaling.id],
            refetchType: "all",
          });
          revalidator.revalidate();
        },
        onError: (error) => {
          if (isValidationError(error)) {
            error.errors.forEach((fieldError: FieldError) => {
              setError(fieldError.detail);
            });
          }
        },
      });
    }
  }

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
                  {skriveTilgang && (
                    <ActionMenu>
                      <ActionMenu.Trigger>
                        <Button variant="primary" size="small">
                          Handlinger
                        </Button>
                      </ActionMenu.Trigger>
                      <ActionMenu.Content>
                        <ActionMenu.Item
                          icon={<PiggybankIcon />}
                          onSelect={() => ekstraTilsagnDefaults()}
                        >
                          Opprett tilsagn
                        </ActionMenu.Item>
                        {avvistUtbetaling && (
                          <ActionMenu.Item
                            icon={<PencilFillIcon />}
                            onSelect={() => setEndreUtbetaling(true)}
                          >
                            Endre utbetaling
                          </ActionMenu.Item>
                        )}
                      </ActionMenu.Content>
                    </ActionMenu>
                  )}
                </HStack>
                {tilsagn.length < 1 ? (
                  <Alert variant="info">Tilsagn mangler</Alert>
                ) : (
                  <Table>
                    <Table.Header>
                      <Table.Row>
                        <Table.HeaderCell />
                        <Table.HeaderCell scope="col">Periodestart</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Periodeslutt</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Kostnadssted</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Tilgjengelig på tilsagn</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Gjør opp tilsagn</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Utbetales</Table.HeaderCell>
                        <Table.HeaderCell scope="col">Status</Table.HeaderCell>
                        <Table.HeaderCell scope="col" />
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {tilsagn.map((t) => {
                        const delutbetaling = delutbetalinger.find((d) => d.tilsagnId == t.id);

                        if (!delutbetaling || delutbetaling.type === "DELUTBETALING_AVVIST") {
                          return (
                            <OpprettDelutbetalingRow
                              key={t.id}
                              id={delutbetaling?.id}
                              tilsagn={t}
                              kanRedigere={kanRedigeres}
                              onDelutbetalingChange={(delutbetaling) =>
                                setDelutbetalingPerTilsagn([
                                  ...delutbetalingPerTilsagn.filter(
                                    (d) => d.tilsagnId !== delutbetaling.tilsagnId,
                                  ),
                                  delutbetaling,
                                ])
                              }
                              opprettelse={delutbetaling?.opprettelse}
                              belop={delutbetaling?.belop ?? 0}
                              frigjorTilsagn={delutbetaling?.frigjorTilsagn ?? false}
                            />
                          );
                        } else
                          return (
                            <DelutbetalingRow
                              key={t.id}
                              tilsagn={t}
                              delutbetaling={delutbetaling}
                              ansatt={ansatt}
                            />
                          );
                      })}
                      <Table.Row>
                        <Table.DataCell
                          className="font-bold"
                          colSpan={5}
                        >{`Beløp arrangør har sendt inn ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
                        <Table.DataCell colSpan={2} className="font-bold">
                          {formaterNOK(totalGjenstaendeBelop)}
                        </Table.DataCell>
                        <Table.DataCell className="font-bold">
                          {formaterNOK(utbetalesTotal)}
                        </Table.DataCell>
                        <Table.DataCell colSpan={2}>
                          <HStack align="center">
                            <CopyButton
                              variant="action"
                              copyText={differanse.toString()}
                              size="small"
                            />
                            <BodyShort weight="semibold">
                              {`Differanse ${formaterNOK(differanse)}`}
                            </BodyShort>
                          </HStack>
                        </Table.DataCell>
                      </Table.Row>
                    </Table.Body>
                  </Table>
                )}
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
                    verdi={utbetaling.betalingsinformasjon?.kid ?? "N/A"}
                  />
                </VStack>
              </VStack>
              <Separator />
              {kanRedigeres && (
                <VStack align="end" gap="4">
                  <HStack>
                    <Button size="small" type="button" onClick={() => sendTilGodkjenning()}>
                      Send til godkjenning
                    </Button>
                  </HStack>
                  {error && (
                    <Alert variant="error" size="small">
                      {error}
                    </Alert>
                  )}
                </VStack>
              )}
            </Box>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
