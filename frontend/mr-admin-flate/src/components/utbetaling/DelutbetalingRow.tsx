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
  DelutbetalingTilGodkjenning,
  DelutbetalingAvvist,
  DelutbetalingOverfortTilUtbetaling,
  DelutbetalingUtbetalt,
  NavAnsattRolle,
} from "@mr/api-client-v2";
import { BodyShort, Button, HStack, Table, TextField } from "@navikt/ds-react";
import { formaterNOK, isValidationError } from "@mr/frontend-common/utils/utils";
import { useRevalidator } from "react-router";
import { useState } from "react";
import { useBesluttDelutbetaling } from "@/api/utbetaling/useBesluttDelutbetaling";
import { AvvistAlert } from "@/pages/gjennomforing/tilsagn/AarsakerAlert";
import { AarsakerOgForklaringModal } from "../modal/AarsakerOgForklaringModal";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { useUpsertDelutbetaling } from "@/api/utbetaling/useUpsertDelutbetaling";
import { Metadata } from "../detaljside/Metadata";
import { createPortal } from "react-dom";

interface Props {
  utbetaling: UtbetalingKompakt;
  tilsagn: TilsagnDto;
  delutbetaling?: DelutbetalingDto;
  ansatt: NavAnsatt;
  onBelopChange: (b: number) => void;
}

export function DelutbetalingRow({
  utbetaling,
  tilsagn,
  delutbetaling,
  ansatt,
  onBelopChange,
}: Props) {
  if (tilsagn.status.type !== "GODKJENT") {
    return <TilsagnIkkeGodkjentRow tilsagn={tilsagn} />;
  }

  switch (delutbetaling?.type) {
    case "DELUTBETALING_OVERFORT_TIL_UTBETALING":
    case "DELUTBETALING_UTBETALT":
      return <GodkjentRow tilsagn={tilsagn} delutbetaling={delutbetaling} />;
    case "DELUTBETALING_TIL_GODKJENNING":
      return (
        <TilGodkjenningRow
          tilsagn={tilsagn}
          delutbetaling={delutbetaling}
          utbetaling={utbetaling}
          ansatt={ansatt}
        />
      );
    case "DELUTBETALING_AVVIST":
    default: // Eller hvis delutbetalingen ikke er opprettet ennå
      return (
        <EditableRow
          tilsagn={tilsagn}
          delutbetaling={delutbetaling}
          utbetaling={utbetaling}
          ansatt={ansatt}
          onBelopChange={onBelopChange}
        />
      );
  }
}

function EditableRow({
  ansatt,
  utbetaling,
  tilsagn,
  delutbetaling,
  onBelopChange,
}: {
  ansatt: NavAnsatt;
  utbetaling: UtbetalingKompakt;
  tilsagn: TilsagnDto;
  delutbetaling?: DelutbetalingAvvist;
  onBelopChange: (b: number) => void;
}) {
  const [belop, setBelop] = useState<number>(delutbetaling?.belop ?? 0);
  const [error, setError] = useState<string | undefined>(undefined);

  const revalidate = useRevalidator();
  const opprettMutation = useUpsertDelutbetaling(utbetaling.id);

  function sendTilGodkjenning() {
    if (error) return;
    const body: DelutbetalingRequest = {
      belop,
      tilsagnId: tilsagn.id,
    };

    opprettMutation.mutate(body, {
      onSuccess: () => {
        revalidate.revalidate();
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
  const skriveTilgang = ansatt?.roller.includes(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV);

  return (
    <Table.ExpandableRow
      defaultOpen={Boolean(delutbetaling)}
      expansionDisabled={!delutbetaling}
      key={tilsagn.id}
      className={delutbetaling ? "bg-surface-warning-subtle" : ""}
      content={
        delutbetaling ? (
          <AvvistAlert
            header="Utbetaling returnert"
            navIdent={delutbetaling.besluttetAv}
            navn=""
            aarsaker={delutbetaling.aarsaker}
            forklaring={delutbetaling.forklaring}
            tidspunkt={delutbetaling.besluttetTidspunkt}
          />
        ) : null
      }
    >
      <Table.DataCell>{formaterDato(tilsagn.periodeStart)}</Table.DataCell>
      <Table.DataCell>{formaterDato(tilsagn.periodeSlutt)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{`${formaterNOK(tilsagn.beregning.output.belop)}`}</Table.DataCell>
      <Table.DataCell>
        <TextField
          readOnly={!skriveTilgang}
          size="small"
          label=""
          error={error}
          hideLabel
          inputMode="numeric"
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
      </Table.DataCell>
      <Table.DataCell>
        {delutbetaling && <DelutbetalingTag delutbetaling={delutbetaling} />}{" "}
      </Table.DataCell>
      <Table.DataCell>
        {skriveTilgang && (
          <Button size="small" type="button" onClick={sendTilGodkjenning}>
            Send beløp til godkjenning
          </Button>
        )}
      </Table.DataCell>
    </Table.ExpandableRow>
  );
}

function GodkjentRow({
  tilsagn,
  delutbetaling,
}: {
  tilsagn: TilsagnDto;
  delutbetaling: DelutbetalingOverfortTilUtbetaling | DelutbetalingUtbetalt;
}) {
  return (
    <Table.ExpandableRow
      content={
        <HStack>
          <Metadata horizontal header="Opprettet av" verdi={delutbetaling.opprettetAv} />
          <Metadata horizontal header="Besluttet av" verdi={delutbetaling.besluttetAv} />
        </HStack>
      }
    >
      <Table.DataCell>{formaterDato(tilsagn.periodeStart)}</Table.DataCell>
      <Table.DataCell>{formaterDato(tilsagn.periodeSlutt)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{formaterNOK(tilsagn.beregning.output.belop)}</Table.DataCell>
      <Table.DataCell>
        <b>{formaterNOK(delutbetaling.belop)}</b>
      </Table.DataCell>
      <Table.DataCell>{<DelutbetalingTag delutbetaling={delutbetaling} />}</Table.DataCell>
      <Table.DataCell></Table.DataCell>
    </Table.ExpandableRow>
  );
}

function TilGodkjenningRow({
  ansatt,
  utbetaling,
  tilsagn,
  delutbetaling,
}: {
  ansatt: NavAnsatt;
  utbetaling: UtbetalingKompakt;
  tilsagn: TilsagnDto;
  delutbetaling: DelutbetalingTilGodkjenning;
}) {
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);

  const revalidate = useRevalidator();
  const besluttMutation = useBesluttDelutbetaling(utbetaling.id);

  function beslutt(body: BesluttDelutbetalingRequest) {
    besluttMutation.mutate(body, {
      onSuccess: () => {
        revalidate.revalidate();
      },
      onError: (error: ProblemDetail) => {
        throw error;
      },
    });
  }

  const kanBeslutte =
    delutbetaling &&
    delutbetaling.opprettetAv !== ansatt.navIdent &&
    ansatt?.roller.includes(NavAnsattRolle.OKONOMI_BESLUTTER);

  return (
    <Table.ExpandableRow expansionDisabled content={null}>
      <Table.DataCell>{formaterDato(tilsagn.periodeStart)}</Table.DataCell>
      <Table.DataCell>{formaterDato(tilsagn.periodeSlutt)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{`${formaterNOK(tilsagn.beregning.output.belop)}`}</Table.DataCell>
      <Table.DataCell>
        <b>{formaterNOK(delutbetaling.belop)}</b>
      </Table.DataCell>
      <Table.DataCell>{<DelutbetalingTag delutbetaling={delutbetaling} />}</Table.DataCell>
      <Table.DataCell>
        {kanBeslutte && (
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
        )}
      </Table.DataCell>
      <Portal>
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
      </Portal>
    </Table.ExpandableRow>
  );
}

export function Portal({ children }: { children: React.ReactNode }) {
  return createPortal(children, document.body);
}

function TilsagnIkkeGodkjentRow({ tilsagn }: { tilsagn: TilsagnDto }) {
  return (
    <Table.Row key={tilsagn.id} className="bg-surface-warning-subtle">
      <Table.DataCell></Table.DataCell>
      <Table.DataCell>{formaterDato(tilsagn.periodeStart)}</Table.DataCell>
      <Table.DataCell>{formaterDato(tilsagn.periodeSlutt)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell></Table.DataCell>
      <Table.DataCell></Table.DataCell>
      <Table.DataCell>
        <BodyShort size="small">
          <b>Tilsagn ikke godkjent</b>
        </BodyShort>
      </Table.DataCell>
      <Table.DataCell></Table.DataCell>
    </Table.Row>
  );
}
