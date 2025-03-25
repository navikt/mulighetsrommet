import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnTypeToString } from "@/utils/Utils";
import { FieldError, UtbetalingLinje } from "@mr/api-client-v2";
import { BodyShort, Button, Checkbox, Table, TextField } from "@navikt/ds-react";
import { useState } from "react";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { NyUtbetalingLinje } from "./RedigerUtbetalingLinjeRows";

interface Props {
  linje: (UtbetalingLinje | NyUtbetalingLinje);
  onChange: (linje: NyUtbetalingLinje) => void;
  onDelete: (id: string) => void;
  errors: FieldError[];
}

export function RedigerDelutbetalingRow({ linje, errors, onChange, onDelete }: Props) {
  const [openRow, setOpenRow] = useState<boolean>(false);
  const [belopError, setBelopError] = useState<string | undefined>(undefined);

  return (
    <Table.ExpandableRow
      onOpenChange={() => setOpenRow(!openRow)}
      open={openRow}
      key={linje.id}
      content={errors.map((error) => (
        <BodyShort className="bg-danger">{error.detail}</BodyShort>
      ))}
    >
      <Table.DataCell>{formaterPeriodeStart(linje.tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{formaterPeriodeSlutt(linje.tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(linje.tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{linje.tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{formaterNOK(linje.tilsagn.belopGjenstaende)}</Table.DataCell>
      <Table.DataCell>
        <Checkbox
          hideLabel
          checked={linje.gjorOppTilsagn}
          onChange={(e) => {
            onChange({
              ...linje,
              gjorOppTilsagn: e.target.checked,
            });
          }}
        >
          Gjør opp tilsagn
        </Checkbox>
      </Table.DataCell>
      <Table.DataCell>
        <TextField
          size="small"
          error={belopError}
          label="Utbetales"
          hideLabel
          inputMode="numeric"
          htmlSize={14}
          onChange={(e) => {
            setBelopError(undefined);
            const num = Number(e.target.value);
            if (isNaN(num)) {
              setBelopError("Må være et tall");
            } else {
              onChange({
                ...linje,
                belop: num,
              });
            }
          }}
          value={linje.belop}
        />
      </Table.DataCell>
      <Table.DataCell>{'status' in linje && <DelutbetalingTag status={linje.status} />}</Table.DataCell>
      <Table.DataCell>
        <Button size="small" variant="primary-neutral" onClick={() => onDelete(linje.id)}>
          Fjern
        </Button>
      </Table.DataCell>
    </Table.ExpandableRow>
  );
}
