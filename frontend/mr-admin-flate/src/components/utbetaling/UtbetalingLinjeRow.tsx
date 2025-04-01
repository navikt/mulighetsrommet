import {
  delutbetalingAarsakTilTekst,
  formaterPeriodeSlutt,
  formaterPeriodeStart,
  tilsagnTypeToString,
} from "@/utils/Utils";
import { DelutbetalingReturnertAarsak, FieldError, UtbetalingLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  BodyShort,
  Checkbox,
  HStack,
  List,
  Table,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { Metadata } from "../detaljside/Metadata";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { Link, useParams } from "react-router";

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
  const { gjennomforingId } = useParams();
  const [belopError, setBelopError] = useState<string | undefined>(undefined);
  const skalApneRad =
    errors.length > 0 || Boolean(linje.opprettelse?.type === "BESLUTTET") || linje.gjorOppTilsagn;
  const [openRow, setOpenRow] = useState(skalApneRad);
  const grayBgClass = grayBackground ? "bg-gray-100" : "";

  useEffect(() => {
    setOpenRow(skalApneRad);
  }, [errors, linje.opprettelse, linje.gjorOppTilsagn, skalApneRad]);

  return (
    <Table.ExpandableRow
      shadeOnHover={false}
      open={openRow}
      onOpenChange={() => setOpenRow(!openRow)}
      key={linje.id}
      className={`${grayBackground ? "[&>td:first-child]:bg-gray-100" : ""}`}
      content={
        <VStack gap="2">
          {linje.opprettelse?.aarsaker && linje.opprettelse.aarsaker.length > 0 ? (
            <VStack>
              <Alert size="small" variant="warning">
                <BodyShort>Linjen ble returnert på grunn av følgende årsaker:</BodyShort>
                <List>
                  {linje.opprettelse.aarsaker.map((error) => (
                    <List.Item>
                      {delutbetalingAarsakTilTekst(error as DelutbetalingReturnertAarsak)}
                    </List.Item>
                  ))}
                </List>
                {linje.opprettelse.forklaring && (
                  <BodyShort>
                    <b>Forklaring:</b> {linje.opprettelse.forklaring}
                  </BodyShort>
                )}
              </Alert>
            </VStack>
          ) : null}
          {errors.length > 0 && (
            <VStack className="bg-[var(--a-surface-danger-subtle)]">
              <Alert size="small" variant="error">
                <BodyShort>Følgende feil må fikses:</BodyShort>
                <List>
                  {errors.map((error) => (
                    <List.Item>{error.detail}</List.Item>
                  ))}
                </List>
              </Alert>
            </VStack>
          )}
          {linje.gjorOppTilsagn && (
            <Alert variant="info">
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
      <Table.DataCell className={grayBgClass}>
        <Link to={`/gjennomforinger/${gjennomforingId}/tilsagn/${linje.tilsagn.id}`}>
          {linje.tilsagn.bestillingsnummer}
        </Link>
      </Table.DataCell>
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
