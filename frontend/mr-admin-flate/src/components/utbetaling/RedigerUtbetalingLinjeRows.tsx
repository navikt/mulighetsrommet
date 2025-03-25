import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { Separator } from "@/components/detaljside/Metadata";
import { formaterDatoSomYYYYMMDD, isValidationError, subtractDays } from "@/utils/Utils";
import {
  DelutbetalingRequest,
  FieldError,
  OpprettDelutbetalingerRequest,
  Prismodell,
  TilsagnDto,
  TilsagnStatus,
  TilsagnType,
  UtbetalingDto,
  UtbetalingLinje,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { PiggybankIcon } from "@navikt/aksel-icons";
import {
  ActionMenu,
  Alert,
  BodyShort,
  Button,
  CopyButton,
  Heading,
  HStack,
  Table,
  VStack,
} from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { v4 as uuidv4 } from "uuid";
import { RedigerDelutbetalingRow } from "./RedigerDelutbetalingRow";
import { useNavigate, useParams } from "react-router";
import { useDeleteDelutbetaling } from "@/api/utbetaling/useDeleteDelutbetaling";

export interface NyUtbetalingLinje {
  id: string;
  gjorOppTilsagn: boolean;
  belop: number;
  tilsagn: TilsagnDto;
}

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  tilsagn: TilsagnDto[];
}

export function RedigerUtbetalingLinjeRows({ linjer, utbetaling, tilsagn }: Props) {
  const { gjennomforingId } = useParams();
  const [linjeState, setLinjeState] = useState<(UtbetalingLinje | NyUtbetalingLinje)[]>(linjer);

  const [error, setError] = useState<FieldError[]>([]);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);
  const deleteMutation = useDeleteDelutbetaling();

  const utbetalesTotal = linjeState.reduce((acc, d) => acc + d.belop, 0);
  const totalGjenstaendeBelop = linjeState.reduce((acc, l) => acc + l.tilsagn.belopGjenstaende, 0);
  const differanse = utbetaling.beregning.belop - utbetalesTotal;

  function opprettEkstraTilsagn() {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    const defaultBelop = tilsagn.length === 0 ? utbetaling.beregning.belop : 0;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${TilsagnType.EKSTRATILSAGN}` +
        `&prismodell=${Prismodell.FRI}` +
        `&belop=${defaultBelop}` +
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${formaterDatoSomYYYYMMDD(subtractDays(utbetaling.periode.slutt, 1))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer}`,
    );
  }

  function leggTilLinjer() {
    const nyeLinjer: NyUtbetalingLinje[] = [];
    tilsagn.forEach((t) => {
      if (t.status === TilsagnStatus.GODKJENT && !linjeState.find((l) => l.tilsagn.id === t.id)) {
        nyeLinjer.push({
          belop: 0,
          tilsagn: t,
          gjorOppTilsagn: false,
          id: uuidv4(),
        });
      }
    });
    setLinjeState([...linjeState, ...nyeLinjer].toSorted((m, n) => m.id.localeCompare(n.id)));
  }

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = linjeState.map((linje) => {
      return {
        id: linje.id,
        belop: linje.belop,
        gjorOppTilsagn: linje.gjorOppTilsagn,
        tilsagnId: linje.tilsagn.id,
      };
    });

    const body: OpprettDelutbetalingerRequest = {
      utbetalingId: utbetaling.id,
      delutbetalinger: delutbetalingReq,
    };

    setError([]);

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: ["utbetaling", utbetaling.id],
          refetchType: "all",
        });
      },
      onError: (error) => {
        if (isValidationError(error)) {
          setError(error.errors);
        }
      },
    });
  }

  return (
    <>
      <VStack>
        <HStack justify="space-between">
          <Heading size="medium">Utbetalingslinjer</Heading>
          <ActionMenu>
            <ActionMenu.Trigger>
              <Button variant="primary" size="small">
                Handlinger
              </Button>
            </ActionMenu.Trigger>
            <ActionMenu.Content>
              <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
                Opprett tilsagn
              </ActionMenu.Item>
              <ActionMenu.Item onSelect={leggTilLinjer}>Legg til linjer</ActionMenu.Item>
            </ActionMenu.Content>
          </ActionMenu>
        </HStack>
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
            {linjeState
              .toSorted((m, n) => m.id.localeCompare(n.id))
              .map((linje, index) => {
                return (
                  <RedigerDelutbetalingRow
                    key={linje.id}
                    linje={linje}
                    onChange={(l) => {
                      setError([]);
                      const remaining = linjeState.filter((d) => d.id !== l.id);
                      setLinjeState([...remaining, l]);
                    }}
                    onDelete={(id) => {
                      setError([]);
                      const remaining = linjeState.filter((d) => d.id !== id);
                      setLinjeState([...remaining]);
                      if (linjer.find((l) => l.id === id)) {
                        deleteMutation.mutate(id);
                      }
                    }}
                    errors={error.filter((f) => f.pointer.startsWith(`/${index}`))}
                  />
                );
              })}
            <Table.Row>
              <Table.DataCell
                className="font-bold"
                colSpan={5}
              >{`${utbetalingTekster.beregning.belop.label}: ${formaterNOK(utbetaling.beregning.belop)}`}</Table.DataCell>
              <Table.DataCell colSpan={2} className="font-bold">
                {formaterNOK(totalGjenstaendeBelop)}
              </Table.DataCell>
              <Table.DataCell className="font-bold">{formaterNOK(utbetalesTotal)}</Table.DataCell>
              <Table.DataCell colSpan={2} className="font-bold">
                <HStack align="center" className="w-80">
                  <CopyButton variant="action" copyText={differanse.toString()} size="small" />
                  <BodyShort weight="semibold">{`Differanse ${formaterNOK(differanse)}`}</BodyShort>
                </HStack>
              </Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </VStack>
      <Separator />
      <VStack align="end" gap="4">
        <HStack>
          <Button size="small" type="button" onClick={() => sendTilGodkjenning()}>
            Send til godkjenning
          </Button>
        </HStack>
        {error.find((f) => f.pointer === "/") && (
          <Alert variant="error" size="small">
            {error.find((f) => f.pointer === "/")!.detail}
          </Alert>
        )}
      </VStack>
    </>
  );
}
