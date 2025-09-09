import { FieldError, ValidationError } from "@mr/api-client-v2";
import {
  DelutbetalingRequest,
  OpprettDelutbetalingerRequest,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinje,
} from "@tiltaksadministrasjon/api-client";
import { FileCheckmarkIcon, PiggybankIcon } from "@navikt/aksel-icons";
import { ActionMenu, Alert, Button, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { useReducer, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { UtbetalingLinjeTable } from "./UtbetalingLinjeTable";
import { UtbetalingLinjeRow } from "./UtbetalingLinjeRow";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import {
  UtbetalingLinjerState,
  UtbetalingLinjerStateAction,
} from "@/pages/gjennomforing/utbetaling/helper";
import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import MindreBelopModal from "./MindreBelopModal";

export interface Props {
  utbetaling: UtbetalingDto;
  handlinger: UtbetalingHandling[];
  linjer: UtbetalingLinje[];
}

export function RedigerUtbetalingLinjeView({ linjer: apiLinjer, utbetaling, handlinger }: Props) {
  const { gjennomforingId } = useParams();
  const [error, setError] = useState<FieldError[]>([]);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);

  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  const tilsagnsTypeFraTilskudd = tilsagnType(utbetaling.tilskuddstype);

  function utbetalingsLinjeReducer(
    state: UtbetalingLinjerState,
    action: UtbetalingLinjerStateAction,
  ): UtbetalingLinjerState {
    switch (action.type) {
      case "RELOAD": {
        return { linjer: apiLinjer };
      }
      case "REMOVE":
        return {
          ...state,
          linjer: state.linjer.filter((l: UtbetalingLinje) => l.id !== action.id),
        };
      case "UPDATE":
        return {
          ...state,
          linjer: state.linjer.map((l: UtbetalingLinje) =>
            l.id === action.linje.id ? action.linje : l,
          ),
        };
      default:
        return state;
    }
  }
  const [{ linjer }, linjerDispatch] = useReducer(utbetalingsLinjeReducer, { linjer: apiLinjer });

  function opprettEkstraTilsagn() {
    const defaultTilsagn = linjer.length === 1 ? linjer[0].tilsagn : undefined;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnsTypeFraTilskudd}` +
        `&periodeStart=${utbetaling.periode.start}` +
        `&periodeSlutt=${yyyyMMddFormatting(subDuration(utbetaling.periode.slutt, { days: 1 }))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer || ""}`,
    );
  }

  async function oppdaterLinjer() {
    return await queryClient
      .invalidateQueries({
        queryKey: [QueryKeys.utbetaling(utbetaling.id), QueryKeys.utbetalingsLinjer(utbetaling.id)],
        refetchType: "all",
      })
      .then(() => linjerDispatch({ type: "RELOAD" }));
  }

  function fjernLinje(id: string) {
    setError([]);
    linjerDispatch({ type: "REMOVE", id });
  }

  function utbetalesTotal(): number {
    return linjer.reduce((acc: number, d: UtbetalingLinje) => acc + d.belop, 0);
  }

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = linjer.map((linje: UtbetalingLinje) => {
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
      begrunnelseMindreBetalt,
    };

    setErrors([]);

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        return await queryClient
          .invalidateQueries({
            queryKey: [QueryKeys.utbetaling(utbetaling.id)],
            refetchType: "all",
          })
          .then(() => linjerDispatch({ type: "RELOAD" }));
      },
      onValidationError: (error: ValidationError) => {
        setErrors(error.errors);
      },
    });
  }

  return (
    <>
      {!linjer.length && (
        <Alert variant="info">Det finnes ingen godkjente tilsagn for utbetalingsperioden</Alert>
      )}
      <VStack>
        <HStack align="end">
          <Heading spacing size="medium" level="2">
            Utbetalingslinjer
          </Heading>
          <Spacer />
          <ActionMenu>
            <ActionMenu.Trigger>
              <Button variant="secondary" size="small">
                Handlinger
              </Button>
            </ActionMenu.Trigger>
            <ActionMenu.Content>
              <ActionMenu.Item icon={<PiggybankIcon />} onSelect={opprettEkstraTilsagn}>
                Opprett {avtaletekster.tilsagn.type(tilsagnsTypeFraTilskudd).toLowerCase()}
              </ActionMenu.Item>
              <ActionMenu.Item icon={<FileCheckmarkIcon />} onSelect={oppdaterLinjer}>
                Hent godkjente tilsagn
              </ActionMenu.Item>
            </ActionMenu.Content>
          </ActionMenu>
        </HStack>
        <UtbetalingLinjeTable
          utbetaling={utbetaling}
          linjer={linjer}
          renderRow={(linje, index) => {
            return (
              <UtbetalingLinjeRow
                key={linje.id}
                linje={linje}
                knappeColumn={
                  <Button
                    size="small"
                    variant="secondary-neutral"
                    onClick={() => fjernLinje(linje.id)}
                  >
                    Fjern
                  </Button>
                }
                grayBackground
                onChange={(updated) => {
                  linjerDispatch({ type: "UPDATE", linje: updated });
                }}
                errors={error.filter(
                  (f) => f.pointer.startsWith(`/${index}`) || f.pointer.includes("totalbelop"),
                )}
              />
            );
          }}
        />
      </VStack>
      <VStack gap="2">
        <HStack justify="end">
          {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
            <Button
              size="small"
              type="button"
              onClick={() => {
                if (utbetalesTotal() < utbetaling.belop) {
                  setMindreBelopModalOpen(true);
                } else {
                  sendTilGodkjenning();
                }
              }}
            >
              Send til attestering
            </Button>
          )}
        </HStack>
        <VStack gap="2" align="end">
          {errors.map((error) => (
            <Alert variant="error" size="small">
              {error.detail}
            </Alert>
          ))}
        </VStack>
      </VStack>
      <MindreBelopModal
        open={mindreBelopModalOpen}
        handleClose={() => setMindreBelopModalOpen(false)}
        onConfirm={() => {
          setMindreBelopModalOpen(false);
          sendTilGodkjenning();
        }}
        begrunnelseOnChange={(e: any) => setBegrunnelseMindreBetalt(e.target.value)}
        belopUtbetaling={utbetalesTotal()}
        belopInnsendt={utbetaling.belop}
      />
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
