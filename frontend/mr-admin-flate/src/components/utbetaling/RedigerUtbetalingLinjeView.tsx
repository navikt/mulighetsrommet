import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { formaterDatoSomYYYYMMDD, subtractDays } from "@/utils/Utils";
import {
  DelutbetalingRequest,
  FieldError,
  OpprettDelutbetalingerRequest,
  Prismodell,
  TilsagnDto,
  TilsagnStatus,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingLinje,
  ValidationError,
} from "@mr/api-client-v2";
import { FileCheckmarkIcon, PiggybankIcon } from "@navikt/aksel-icons";
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

function genrererUtbetalingLinjer(tilsagn: TilsagnDto[]): UtbetalingLinje[] {
  return tilsagn
    .filter((t) => t.status === TilsagnStatus.GODKJENT)
    .map((t) => ({
      belop: 0,
      tilsagn: t,
      gjorOppTilsagn: false,
      id: uuidv4(),
    }));
}

export function RedigerUtbetalingLinjeView({ linjer, utbetaling, tilsagn }: Props) {
  const { gjennomforingId } = useParams();
  const [linjerState, setLinjerState] = useState<UtbetalingLinje[]>(() =>
    linjer.length === 0 ? genrererUtbetalingLinjer(tilsagn) : linjer,
  );

  const [error, setError] = useState<FieldError[]>([]);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  function opprettEkstraTilsagn() {
    const defaultTilsagn = tilsagn.length === 1 ? tilsagn[0] : undefined;
    const defaultBelop = tilsagn.length === 0 ? utbetaling.beregning.belop : 0;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnType(utbetaling.tilskuddstype)}` +
        `&prismodell=${Prismodell.FRI}` +
        `&belop=${defaultBelop}` +
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${formaterDatoSomYYYYMMDD(subtractDays(utbetaling.periode.slutt, 1))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer}`,
    );
  }

  function leggTilLinjer() {
    const nyeLinjer = genrererUtbetalingLinjer(tilsagn).filter(
      (linje) => !linjerState.find((l) => l.tilsagn.id === linje.tilsagn.id),
    );
    setLinjerState([...linjerState, ...nyeLinjer].toSorted((m, n) => m.id.localeCompare(n.id)));
  }

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = linjerState.map((linje) => {
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
      onValidationError: (error: ValidationError) => {
        setError(error.errors);
      },
    });
  }

  function fjernLinje(id: string) {
    setError([]);
    const remaining = linjerState.filter((d) => d.id !== id);
    setLinjerState([...remaining]);
  }

  return (
    <>
      <VStack>
        <HStack justify="space-between">
          <Heading spacing size="medium" level="2">
            Utbetalingslinjer
          </Heading>
          <ActionMenu>
            <ActionMenu.Trigger>
              <Button variant="secondary" size="small">
                Handlinger
              </Button>
            </ActionMenu.Trigger>
            <ActionMenu.Content>
              <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
                Opprett tilsagn
              </ActionMenu.Item>
              <ActionMenu.Item icon={<FileCheckmarkIcon />} onSelect={leggTilLinjer}>
                Hent godkjente tilsagn
              </ActionMenu.Item>
            </ActionMenu.Content>
          </ActionMenu>
        </HStack>
        <UtbetalingLinjeTable
          utbetaling={utbetaling}
          linjer={linjerState}
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
                grayBackground
                onChange={(updated) => {
                  setLinjerState((prev) =>
                    prev.map((linje) => (linje.id === updated.id ? updated : linje)),
                  );
                }}
                errors={error.filter(
                  (f) => f.pointer.startsWith(`/${index}`) || f.pointer.includes("totalbelop"),
                )}
              />
            );
          }}
        />
      </VStack>
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

function tilsagnType(tilskuddstype: Tilskuddstype): TilsagnType {
  switch (tilskuddstype) {
    case Tilskuddstype.TILTAK_DRIFTSTILSKUDD:
      return TilsagnType.EKSTRATILSAGN;
    case Tilskuddstype.TILTAK_INVESTERINGER:
      return TilsagnType.INVESTERING;
  }
}
