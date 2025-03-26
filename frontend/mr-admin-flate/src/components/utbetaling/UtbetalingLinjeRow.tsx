import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnTypeToString } from "@/utils/Utils";
import { FieldError, UtbetalingLinje } from "@mr/api-client-v2";
import { BodyShort, Checkbox, Table, TextField, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

interface Props {
  readOnly?: boolean;
  linje: UtbetalingLinje;
  knappeColumn: React.ReactNode;
  onChange?: (linje: UtbetalingLinje) => void;
  errors?: FieldError[];
}

export function UtbetalingLinjeRow({
  linje,
  errors = [],
  onChange,
  knappeColumn,
  readOnly = false,
}: Props) {
  const [belopError, setBelopError] = useState<string | undefined>(undefined);

  return (
    <Table.ExpandableRow
      open={errors.length > 0}
      key={linje.id}
      content={
        <VStack className="bg-[var(--a-surface-danger-subtle)]">
          {errors.map((error) => (
            <BodyShort>{error.detail}</BodyShort>
          ))}
        </VStack>
      }
    >
      <Table.DataCell>{formaterPeriodeStart(linje.tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{formaterPeriodeSlutt(linje.tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{tilsagnTypeToString(linje.tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{linje.tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>{formaterNOK(linje.tilsagn.belopGjenstaende)}</Table.DataCell>
      <Table.DataCell>
        <Checkbox
          hideLabel
          readOnly={readOnly}
          checked={linje.gjorOppTilsagn}
          onChange={(e) => {
            onChange?.({
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
          readOnly={readOnly}
          hideLabel
          inputMode="numeric"
          htmlSize={14}
          onChange={(e) => {
            setBelopError(undefined);
            const num = Number(e.target.value);
            if (isNaN(num)) {
              setBelopError("Må være et tall");
            } else {
              onChange?.({
                ...linje,
                belop: num,
              });
            }
          }}
          value={linje.belop}
        />
      </Table.DataCell>
      <Table.DataCell>{linje.status && <DelutbetalingTag status={linje.status} />}</Table.DataCell>
      <Table.DataCell>{knappeColumn}</Table.DataCell>
    </Table.ExpandableRow>
  );
}
