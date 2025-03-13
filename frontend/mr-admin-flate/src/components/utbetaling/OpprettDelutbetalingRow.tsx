import { formaterDato, tilsagnTypeToString } from "@/utils/Utils";
import { TilsagnDto, TilsagnStatus, Totrinnskontroll } from "@mr/api-client-v2";
import { Alert, Checkbox, Table, TextField } from "@navikt/ds-react";
import { useState } from "react";
import { AvvistAlert } from "@/pages/gjennomforing/tilsagn/AarsakerAlert";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { TilsagnStatusTag } from "./TilsagnTag";

interface Props {
  id?: string;
  tilsagn: TilsagnDto;
  belop: number;
  frigjorTilsagn: boolean;
  opprettelse?: Totrinnskontroll;
  kanRedigere: boolean;
  onDelutbetalingChange: (d: {
    id?: string;
    tilsagnId: string;
    belop: number;
    frigjorTilsagn: boolean;
  }) => void;
}

export function OpprettDelutbetalingRow({
  id,
  tilsagn,
  belop,
  frigjorTilsagn,
  opprettelse,
  kanRedigere,
  onDelutbetalingChange,
}: Props) {
  const [endretBelop, setEndretBelop] = useState<number>(belop);
  const [endretFrigjorTilsagn, setEndretFrigjorTilsagn] = useState<boolean>(frigjorTilsagn);
  const [openRow, setOpenRow] = useState<boolean>(frigjorTilsagn);
  const [error, setError] = useState<string | undefined>(undefined);

  const godkjentTilsagn = tilsagn.status === TilsagnStatus.GODKJENT;

  function handleOnChange(belop: number, frigjorTilsagn: boolean) {
    onDelutbetalingChange({
      id: id,
      tilsagnId: tilsagn.id,
      belop: belop,
      frigjorTilsagn: frigjorTilsagn,
    });
  }
  const cellClass = error && "align-top";
  return (
    <Table.ExpandableRow
      defaultOpen={!!opprettelse}
      onOpenChange={() => setOpenRow(!openRow)}
      open={openRow}
      expansionDisabled={!frigjorTilsagn}
      key={tilsagn.id}
      content={
        <>
          {opprettelse && (
            <AvvistAlert
              header="Utbetaling returnert"
              navIdent={opprettelse?.besluttetAv}
              aarsaker={opprettelse?.aarsaker}
              forklaring={opprettelse?.forklaring}
              tidspunkt={opprettelse?.besluttetTidspunkt}
            />
          )}
          {frigjorTilsagn && (
            <Alert variant="warning">
              Når denne utbetalingen godkjennes vil det ikke lenger være mulig å sende inn nye
              utbetalinger fra dette tilsagnet
            </Alert>
          )}
        </>
      }
    >
      <Table.DataCell className={cellClass}>{formaterDato(tilsagn.periodeStart)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{formaterDato(tilsagn.periodeSlutt)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{tilsagnTypeToString(tilsagn.type)}</Table.DataCell>
      <Table.DataCell className={cellClass}>{tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell className={cellClass}>
        {formaterNOK(tilsagn.beregning.output.belop)}
      </Table.DataCell>
      <Table.DataCell className={cellClass}>
        <Checkbox
          hideLabel
          readOnly={!kanRedigere}
          checked={endretFrigjorTilsagn}
          onChange={(e) => {
            setEndretFrigjorTilsagn(e.target.checked);
            handleOnChange(belop, e.target.checked);
          }}
        >
          Gjør opp tilsagn
        </Checkbox>
      </Table.DataCell>
      <Table.DataCell className={error && "align-top"}>
        {godkjentTilsagn && kanRedigere ? (
          <TextField
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
              } else {
                setEndretBelop(num);
                handleOnChange(num, frigjorTilsagn);
              }
            }}
            value={endretBelop}
          />
        ) : (
          (belop ?? "-")
        )}
      </Table.DataCell>
      <Table.DataCell className={error && "align-top pt-2"} colSpan={2}>
        <>
          {opprettelse ? (
            <DelutbetalingTag type={"DELUTBETALING_AVVIST"} />
          ) : (
            <TilsagnStatusTag status={tilsagn.status} />
          )}
        </>
      </Table.DataCell>
    </Table.ExpandableRow>
  );
}
