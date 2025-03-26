import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnTypeToString } from "@/utils/Utils";
import { FieldError, UtbetalingLinje } from "@mr/api-client-v2";
import { Alert, BodyShort, Checkbox, HStack, Table, TextField, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Metadata } from "../detaljside/Metadata";

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
      open={errors.length > 0 || Boolean(linje.opprettelse) || linje.gjorOppTilsagn}
      key={linje.id}
      content={
        <VStack gap="2">
          <VStack className="bg-[var(--a-surface-danger-subtle)]">
            {errors.map((error) => (
              <BodyShort>{error.detail}</BodyShort>
            ))}
          </VStack>
          {linje.gjorOppTilsagn && (
            <Alert variant="warning">
              Når denne utbetalingen godkjennes av beslutter vil det ikke lenger være mulig å gjøre
              flere utbetalinger fra tilsagnet
            </Alert>
          )}
          {linje.opprettelse && (
            <HStack gap="4">
              <Metadata horizontal header="Behandlet av" verdi={linje.opprettelse.behandletAv} />
              {linje.opprettelse.type === "BESLUTTET" && (
                <Metadata horizontal header="Besluttet av" verdi={linje.opprettelse.besluttetAv} />
              )}
            </HStack>
          )}
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
