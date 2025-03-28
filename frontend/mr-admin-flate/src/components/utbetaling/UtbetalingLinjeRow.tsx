import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnTypeToString } from "@/utils/Utils";
import { FieldError, UtbetalingLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, BodyShort, Checkbox, HStack, Table, TextField, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { Metadata } from "../detaljside/Metadata";
import { DelutbetalingTag } from "./DelutbetalingTag";

interface Props {
  readOnly?: boolean;
  linje: UtbetalingLinje;
  knappeColumn?: React.ReactNode;
  onChange?: (linje: UtbetalingLinje) => void;
  errors?: FieldError[];
  grayBackground?: boolean;
}

export function UtbetalingLinjeRow({
  linje,
  errors = [],
  onChange,
  knappeColumn,
  readOnly = false,
  grayBackground = false,
}: Props) {
  const [belopError, setBelopError] = useState<string | undefined>(undefined);
  const [openRow, setOpenRow] = useState(
    errors.length > 0 || Boolean(linje.opprettelse) || linje.gjorOppTilsagn,
  );
  const grayBgClass = grayBackground ? "bg-gray-100" : "";
  return (
    <Table.ExpandableRow
      shadeOnHover={false}
      open={openRow}
      onOpenChange={() => setOpenRow(!openRow)}
      onClick={() => setOpenRow(!openRow)}
      key={linje.id}
      className={`${grayBackground ? "[&>td:first-child]:bg-gray-100" : ""}`}
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
              <Metadata header="Behandlet av" verdi={linje.opprettelse.behandletAv} />
              {linje.opprettelse.type === "BESLUTTET" && (
                <Metadata header="Besluttet av" verdi={linje.opprettelse.besluttetAv} />
              )}
            </HStack>
          )}
        </VStack>
      }
    >
      <Table.DataCell className={grayBgClass}>{linje.tilsagn.bestillingsnummer}</Table.DataCell>
      <Table.DataCell className={grayBgClass}>
        {tilsagnTypeToString(linje.tilsagn.type)}
      </Table.DataCell>
      <Table.DataCell className={grayBgClass}>
        {formaterPeriodeStart(linje.tilsagn.periode)}
      </Table.DataCell>
      <Table.DataCell className={grayBgClass}>
        {formaterPeriodeSlutt(linje.tilsagn.periode)}
      </Table.DataCell>
      <Table.DataCell className={grayBgClass}>{linje.tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell className={grayBgClass}>
        {formaterNOK(linje.tilsagn.belopGjenstaende)}
      </Table.DataCell>
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
      <Table.DataCell>
        {knappeColumn || (linje.status && <DelutbetalingTag status={linje.status} />)}
      </Table.DataCell>
    </Table.ExpandableRow>
  );
}
