import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { Header } from "@/components/detaljside/Header";
import { Metadata, MetadataHorisontal, Separator } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DelutbetalingRow } from "@/components/utbetaling/DelutbetalingRow";
import { OpprettDelutbetalingRow } from "@/components/utbetaling/OpprettDelutbetalingRow";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import {
  formaterDato,
  formaterDatoSomYYYYMMDD,
  formaterPeriode,
  isValidationError,
  subtractDays,
} from "@/utils/Utils";
import {
  DelutbetalingRequest,
  DelutbetalingStatus,
  FieldError,
  NavAnsattRolle,
  OpprettDelutbetalingerRequest,
  Prismodell,
  TilsagnStatus,
  TilsagnType,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
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
import { useNavigate, useParams } from "react-router";
import { v4 as uuidv4 } from "uuid";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import {
  tilsagnTilUtbetalingQuery,
  utbetalingHistorikkQuery,
  utbetalingQuery,
} from "./utbetalingPageLoader";

import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";

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
    delutbetalinger: utbetaling.delutbetalinger,
  };
}

export interface NyDelutbetaling {
  id?: string;
  tilsagnId: string;
  belop: number;
  gjorOppTilsagn: boolean;
  status?: DelutbetalingStatus;
}

export function UtbetalingPage() {
  const { gjennomforingId } = useParams();
  const { gjennomforing, ansatt, historikk, tilsagn, utbetaling, delutbetalinger } =
    useUtbetalingPageData();

  const [delutbetalingPerTilsagn, setDelutbetalingPerTilsagn] = useState<NyDelutbetaling[]>(
    delutbetalinger.map(({ delutbetaling }) => {
      return {
        id: delutbetaling.id,
        tilsagnId: delutbetaling.tilsagnId,
        belop: delutbetaling.belop,
        gjorOppTilsagn: delutbetaling.gjorOppTilsagn,
        status: delutbetaling.status,
      };
    }),
  );

  const [endreUtbetaling, setEndreUtbetaling] = useState<boolean>(delutbetalinger.length === 0);
  const [error, setError] = useState<string | undefined>(undefined);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  const skriveTilgang = ansatt?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV);
  const avvistUtbetaling = delutbetalinger.find(
    ({ delutbetaling }) => delutbetaling.status === DelutbetalingStatus.RETURNERT,
  );
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
    .reduce((acc, t) => acc + t.belopGjenstaende, 0);

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
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${formaterDatoSomYYYYMMDD(subtractDays(utbetaling.periode.slutt, 1))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer}`,
    );
  }

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = delutbetalingPerTilsagn
      .filter((d) => d.status === DelutbetalingStatus.RETURNERT || d.status === undefined)
      .map((d) => {
        return {
          ...d,
          id: d.id ?? uuidv4(),
        };
      });

    const body: OpprettDelutbetalingerRequest = {
      utbetalingId: utbetaling.id,
      delutbetalinger: delutbetalingReq,
    };

    setError(undefined);

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        setEndreUtbetaling(false);
        await queryClient.invalidateQueries({
          queryKey: ["utbetaling", utbetaling.id],
          refetchType: "all",
        });
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
                        const { delutbetaling, opprettelse } =
                          delutbetalinger.find(
                            ({ delutbetaling }) => delutbetaling.tilsagnId == t.id,
                          ) ?? {};

                        if (
                          !delutbetaling ||
                          !opprettelse ||
                          delutbetaling.status === DelutbetalingStatus.RETURNERT
                        ) {
                          return (
                            <OpprettDelutbetalingRow
                              key={t.id}
                              id={delutbetaling?.id}
                              tilsagn={t}
                              status={delutbetaling?.status}
                              kanRedigere={kanRedigeres}
                              onDelutbetalingChange={(delutbetaling) => {
                                const remaining = delutbetalingPerTilsagn.filter(
                                  (d) => d.tilsagnId !== delutbetaling.tilsagnId,
                                );
                                setDelutbetalingPerTilsagn([...remaining, delutbetaling]);
                              }}
                              opprettelse={opprettelse}
                              belop={delutbetaling?.belop ?? 0}
                              gjorOppTilsagn={delutbetaling?.gjorOppTilsagn ?? false}
                            />
                          );
                        } else {
                          return (
                            <DelutbetalingRow
                              key={t.id}
                              ansatt={ansatt}
                              tilsagn={t}
                              delutbetaling={delutbetaling}
                              opprettelse={opprettelse}
                            />
                          );
                        }
                      })}
                      <Table.Row>
                        <Table.DataCell
                          className="font-bold"
                          colSpan={5}
                        >{`${utbetalingTekster.beregning.belop.label}: ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
                        <Table.DataCell colSpan={2} className="font-bold">
                          {formaterNOK(totalGjenstaendeBelop)}
                        </Table.DataCell>
                        <Table.DataCell className="font-bold">
                          {formaterNOK(utbetalesTotal)}
                        </Table.DataCell>
                        <Table.DataCell colSpan={2} className="font-bold">
                          <HStack align="center" className="w-80">
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
                    verdi={utbetaling.betalingsinformasjon?.kid || "-"}
                  />
                </VStack>
              </VStack>
              {kanRedigeres && (
                <>
                  <Separator />
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
                </>
              )}
            </Box>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
