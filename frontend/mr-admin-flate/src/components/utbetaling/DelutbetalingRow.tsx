import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { AvvistAlert } from "@/pages/gjennomforing/tilsagn/AarsakerAlert";
import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnTypeToString } from "@/utils/Utils";
import {
  BesluttDelutbetalingRequest,
  Besluttelse,
  DelutbetalingDto,
  NavAnsatt,
  NavAnsattRolle,
  ProblemDetail,
  TilsagnDto,
  UtbetalingKompakt,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, BodyShort, Button, Checkbox, HStack, Table, TextField } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Metadata } from "../detaljside/Metadata";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { DelutbetalingTag } from "./DelutbetalingTag";

interface Props {
  utbetaling: UtbetalingKompakt;
  tilsagn: TilsagnDto;
  delutbetaling?: DelutbetalingDto;
  ansatt: NavAnsatt;
  endreUtbetaling: boolean;
  onBelopChange: (b: number) => void;
  onFrigjorTilsagnChange: (f: boolean) => void;
}

export function DelutbetalingRow({
  tilsagn,
  delutbetaling,
  ansatt,
  endreUtbetaling,
  onBelopChange,
  onFrigjorTilsagnChange,
}: Props) {
  const [belop, setBelop] = useState<number>(delutbetaling?.belop ?? 0);
  const [frigjorTilsagn, setFrigjorTilsagn] = useState<boolean>(
    delutbetaling?.frigjorTilsagn ?? false,
  );
  const [open, setOpen] = useState<boolean>(false);
  const [error, setError] = useState<string | undefined>(undefined);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const queryClient = useQueryClient();

  const besluttMutation = useBesluttDelutbetaling(delutbetaling?.id ?? "");

  const kanBeslutte =
    delutbetaling &&
    delutbetaling.opprettelse.behandletAv !== ansatt.navIdent &&
    ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER);
  const skriveTilgang = ansatt?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV);

  const godkjentTilsagn = tilsagn.status === "GODKJENT";
  const godkjentUtbetaling =
    delutbetaling?.type === "DELUTBETALING_OVERFORT_TIL_UTBETALING" ||
    delutbetaling?.type === "DELUTBETALING_UTBETALT";
  const avvist = delutbetaling?.type === "DELUTBETALING_AVVIST";
  const kanRedigere =
    skriveTilgang && godkjentTilsagn && (!delutbetaling || (avvist && endreUtbetaling));

  function beslutt(body: BesluttDelutbetalingRequest) {
    besluttMutation.mutate(body, {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ["utbetaling"] });
      },
      onError: (error: ProblemDetail) => {
        throw error;
      },
    });
  }

  function onFrigjorCheck(frigjor: boolean) {
    setOpen(frigjor);
    onFrigjorTilsagnChange(frigjor);
    setFrigjorTilsagn(frigjor);
  }

  function content() {
    if (frigjorTilsagn && !godkjentUtbetaling)
      return (
        <Alert variant="warning">
          Når denne utbetalingen godkjennes vil det ikke lengre være mulig å sende inn nye
          utbetalinger fra dette tilsagnet
        </Alert>
      );
    if (avvist)
      return (
        <AvvistAlert
          header="Utbetaling returnert"
          navIdent={delutbetaling.opprettelse.besluttetAv}
          aarsaker={delutbetaling.opprettelse.aarsaker}
          forklaring={delutbetaling.opprettelse.forklaring}
          tidspunkt={delutbetaling.opprettelse.besluttetTidspunkt}
        />
      );
    else if (godkjentUtbetaling)
      return (
        <HStack>
          <Metadata
            horizontal
            header="Behandlet av"
            verdi={delutbetaling.opprettelse.behandletAv}
          />
          <Metadata
            horizontal
            header="Besluttet av"
            verdi={delutbetaling.opprettelse.besluttetAv}
          />
        </HStack>
      );
    else return null;
  }

  function utbetales() {
    if (!godkjentTilsagn) return "-";
    else if (kanRedigere)
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
    else return formaterNOK(delutbetaling?.belop ?? 0);
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
    if (kanBeslutte && delutbetaling?.type === "DELUTBETALING_TIL_GODKJENNING")
      return (
        <HStack gap="4">
          <Button
            size="small"
            type="button"
            onClick={() =>
              beslutt({
                besluttelse: Besluttelse.GODKJENT,
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
      onOpenChange={() => setOpen(!open)}
      open={open}
      expansionDisabled={!frigjorTilsagn}
      key={tilsagn.id}
      content={content()}
    >
      <Table.DataCell className={cellClass}>{formaterPeriodeStart(tilsagn.periode)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{formaterPeriodeSlutt(tilsagn.periode)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell className={cellClass}>
        {formaterNOK(tilsagn.beregning.output.belop)}
      </Table.DataCell>
      <Table.DataCell className={cellClass}>
        <Checkbox
          hideLabel
          readOnly={!kanRedigere}
          checked={frigjorTilsagn}
          onChange={(e) => onFrigjorCheck(e.target.checked)}
        >
          Gjør opp tilsagn
        </Checkbox>
      </Table.DataCell>
      <Table.DataCell className={cellClass}>{utbetales()}</Table.DataCell>
      <Table.DataCell className={error && "align-top pt-2"}>{tag()}</Table.DataCell>
      <Table.DataCell className={cellClass}>{handlinger()}</Table.DataCell>
      {delutbetaling && (
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
              aarsaker,
              forklaring: forklaring ?? null,
            });
            setAvvisModalOpen(false);
          }}
        />
      )}
    </Table.ExpandableRow>
  );
}
