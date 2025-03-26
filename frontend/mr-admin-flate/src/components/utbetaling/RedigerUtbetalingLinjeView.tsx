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
import { PiggybankIcon } from "@navikt/aksel-icons";
import { ActionMenu, Alert, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { useNavigate, useParams } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";

export interface Props {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  tilsagn: TilsagnDto[];
}

export function RedigerUtbetalingLinjeView({ linjer, utbetaling, tilsagn }: Props) {
  const { gjennomforingId } = useParams();
  const [linjeState, setLinjeState] = useState<UtbetalingLinje[]>(linjer);

  const [error, setError] = useState<FieldError[]>([]);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

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
    const nyeLinjer: UtbetalingLinje[] = [];
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

  function fjernLinje(id: string) {
    setError([]);
    const remaining = linjeState.filter((d) => d.id !== id);
    setLinjeState([...remaining]);
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
        <UtbetalingLinjeTable
          utbetaling={utbetaling}
          linjer={linjeState}
          renderRow={(linje, index) => {
            return (
              <UtbetalingLinjeRow
                key={linje.id}
                linje={linje}
                knappeColumn={
                  <Button
                    size="small"
                    variant="primary-neutral"
                    onClick={() => fjernLinje(linje.id)}
                  >
                    Fjern
                  </Button>
                }
                onChange={(updated) => {
                  setLinjeState((prev) =>
                    prev.map((linje) => (linje.id === updated.id ? updated : linje)),
                  );
                }}
                errors={error.filter((f) => f.pointer.startsWith(`/${index}`))}
              />
            );
          }}
        />
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
