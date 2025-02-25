import { formaterDato, tilsagnTypeToString } from "@/utils/Utils";
import {
  UtbetalingKompakt,
  TilsagnDto,
  DelutbetalingDto,
  ProblemDetail,
  FieldError,
  DelutbetalingRequest,
  Besluttelse,
  BesluttDelutbetalingRequest,
  NavAnsatt,
  NavAnsattRolle,
} from "@mr/api-client-v2";
import { BodyShort, Button, HStack, Table, TextField } from "@navikt/ds-react";
import { formaterNOK, isValidationError } from "@mr/frontend-common/utils/utils";
import { useState } from "react";
import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { AvvistAlert } from "@/pages/gjennomforing/tilsagn/AarsakerAlert";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { useUpsertDelutbetaling } from "@/api/utbetaling/useUpsertDelutbetaling";
import { Metadata } from "../detaljside/Metadata";
import { useQueryClient } from "@tanstack/react-query";
import { useRevalidator } from "react-router";

interface Props {
  utbetaling: UtbetalingKompakt;
  tilsagn: TilsagnDto;
  delutbetaling?: DelutbetalingDto;
  ansatt: NavAnsatt;
  endreUtbetaling: boolean;
  onBelopChange: (b: number) => void;
}

export function DelutbetalingRow({
  utbetaling,
  tilsagn,
  delutbetaling,
  ansatt,
  endreUtbetaling,
  onBelopChange,
}: Props) {
  const [belop, setBelop] = useState<number>(delutbetaling?.belop ?? 0);
  const [error, setError] = useState<string | undefined>(undefined);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);

  const revalidator = useRevalidator();
  const queryClient = useQueryClient();
  const opprettMutation = useUpsertDelutbetaling(utbetaling.id);
  const besluttMutation = useBesluttDelutbetaling(utbetaling.id);

  const kanBeslutte =
    delutbetaling &&
    delutbetaling.opprettetAv !== ansatt.navIdent &&
    ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER);
  const skriveTilgang = ansatt?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV);

  const godkjentTilsagn = tilsagn.status.type === "GODKJENT";
  const avvist = delutbetaling?.type === "DELUTBETALING_AVVIST";
  const tilGodkjenning = delutbetaling?.type === "DELUTBETALING_TIL_GODKJENNING";
  const godkjentUtbetaling =
    delutbetaling?.type === "DELUTBETALING_OVERFORT_TIL_UTBETALING" ||
    delutbetaling?.type === "DELUTBETALING_UTBETALT";

  function sendTilGodkjenning() {
    if (error) return;
    const body: DelutbetalingRequest = {
      belop,
      tilsagnId: tilsagn.id,
    };

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: ["utbetaling", utbetaling.id],
          refetchType: "all",
        });
        revalidator.revalidate();
      },
      onError: (error: ProblemDetail) => {
        if (isValidationError(error)) {
          error.errors.forEach((fieldError: FieldError) => {
            setError(fieldError.detail);
          });
        }
      },
    });
  }

  function beslutt(body: BesluttDelutbetalingRequest) {
    besluttMutation.mutate(body, {
      onSuccess: () => {
        revalidator.revalidate();
      },
      onError: (error: ProblemDetail) => {
        throw error;
      },
    });
  }

  function content() {
    if (delutbetaling && avvist)
      return (
        <AvvistAlert
          header="Utbetaling returnert"
          navIdent={delutbetaling.besluttetAv}
          navn=""
          aarsaker={delutbetaling.aarsaker}
          forklaring={delutbetaling.forklaring}
          tidspunkt={delutbetaling.besluttetTidspunkt}
        />
      );
    else if (godkjentUtbetaling)
      return (
        <HStack>
          <Metadata horizontal header="Opprettet av" verdi={delutbetaling.opprettetAv} />
          <Metadata horizontal header="Besluttet av" verdi={delutbetaling.besluttetAv} />
        </HStack>
      );
    else return null;
  }

  function utbetales() {
    if (!delutbetaling || (avvist && endreUtbetaling))
      return (
        <TextField
          readOnly={!skriveTilgang}
          size="small"
          error={error}
          label="Utbetales"
          hideLabel
          inputMode="numeric"
          htmlSize={14}
          onChange={(e) => {
            setError(undefined);
            const num = Number(e.target.value);
            if (isNaN(num)) {
              setError("Må være et tall");
            } else if (num > 2_147_483_647) {
              setError("Beløp er for høyt");
            } else {
              setBelop(num);
              onBelopChange(num);
            }
          }}
          value={belop}
        />
      );
    else if (!godkjentTilsagn) return null;
    else return formaterNOK(delutbetaling.belop);
  }

  function tag() {
    if (!godkjentTilsagn)
      return (
        <BodyShort size="small" weight="semibold">
          Tilsagn ikke godkjent
        </BodyShort>
      );
    else if (delutbetaling) return <DelutbetalingTag delutbetaling={delutbetaling} />;
    else return null;
  }

  function handlinger() {
    if (skriveTilgang && godkjentTilsagn && (!delutbetaling || (avvist && endreUtbetaling)))
      return (
        <Button size="small" type="button" onClick={() => sendTilGodkjenning()}>
          Send beløp til godkjenning
        </Button>
      );
    else if (kanBeslutte && tilGodkjenning)
      return (
        <HStack gap="4">
          <Button
            size="small"
            type="button"
            onClick={() =>
              beslutt({
                besluttelse: Besluttelse.GODKJENT,
                tilsagnId: tilsagn.id,
              })
            }
          >
            Godkjenn
          </Button>
          <Button
            variant="secondary"
            size="small"
            type="button"
            onClick={() => setAvvisModalOpen(true)}
          >
            Send i retur
          </Button>
        </HStack>
      );
    else return null;
  }

  const cellClass = error && "align-top";

  return (
    <Table.ExpandableRow
      defaultOpen={avvist}
      expansionDisabled={!delutbetaling}
      key={tilsagn.id}
      content={content()}
    >
      <Table.DataCell className={cellClass}>{formaterDato(tilsagn.periodeStart)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{formaterDato(tilsagn.periodeSlutt)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell className={cellClass}>
        {formaterNOK(tilsagn.beregning.output.belop)}
      </Table.DataCell>
      <Table.DataCell className={cellClass}>{utbetales()}</Table.DataCell>
      <Table.DataCell className={error && "align-top pt-2"}>{tag()}</Table.DataCell>
      <Table.DataCell className={cellClass}>{handlinger()}</Table.DataCell>
      <AarsakerOgForklaringModal
        open={avvisModalOpen}
        header="Send i retur med forklaring"
        buttonLabel="Send i retur"
        aarsaker={[
          { value: "FEIL_BELOP", label: "Feil beløp" },
          { value: "FEIL_ANNET", label: "Annet" },
        ]}
        onClose={() => setAvvisModalOpen(false)}
        onConfirm={({ aarsaker, forklaring }) => {
          beslutt({
            besluttelse: Besluttelse.AVVIST,
            tilsagnId: tilsagn.id,
            aarsaker,
            forklaring: forklaring ?? null,
          });
          setAvvisModalOpen(false);
        }}
      />
    </Table.ExpandableRow>
  );
}
