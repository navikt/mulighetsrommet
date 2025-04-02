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
  HelpText,
  HStack,
  List,
  Table,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { AarsakerOgForklaring } from "../../pages/gjennomforing/tilsagn/AarsakerOgForklaring";
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
  const { gjennomforingId } = useParams();
  const [belopError, setBelopError] = useState<string | undefined>();
  const skalApneRad =
    filterBelopErrors(errors).length > 0 || Boolean(linje.opprettelse?.type === "BESLUTTET");
  const [openRow, setOpenRow] = useState(skalApneRad);
  const grayBgClass = grayBackground ? "bg-gray-100" : "";

  function filterBelopErrors(errors: FieldError[]) {
    return errors.filter((e) => !e.pointer.includes("belop"));
  }

  useEffect(() => {
    setOpenRow(skalApneRad);
  }, [errors, linje.opprettelse, linje.gjorOppTilsagn, skalApneRad]);

  useEffect(() => {
    setBelopError(errors.find((e) => e.pointer.includes("belop"))?.detail);
  }, [belopError, errors]);

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
              <AarsakerOgForklaring
                aarsaker={linje.opprettelse.aarsaker.map((aarsak) =>
                  delutbetalingAarsakTilTekst(aarsak as DelutbetalingReturnertAarsak),
                )}
                forklaring={linje.opprettelse.forklaring}
                heading="Linjen ble returnert på grunn av følgende årsaker:"
              />
            </VStack>
          ) : null}
          {errors.filter((e) => !e.pointer.includes("belop")).length > 0 && (
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
      <Table.DataCell colSpan={2}>
        <HStack gap="2" align="start">
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
          <HelpText>
            Hvis du huker av for å gjøre opp tilsagnet, betyr det at det ikke kan gjøres flere
            utbetalinger på tilsagnet etter at denne utbetalingen er attestert
          </HelpText>
        </HStack>
      </Table.DataCell>
      <Table.DataCell colSpan={2}>
        <TextField
          size="small"
          error={belopError}
          label="Utbetales"
          readOnly={readOnly}
          hideLabel
          className="w-60"
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
      <Table.DataCell colSpan={2} align="right">
        {knappeColumn || (linje.status && <DelutbetalingTag status={linje.status} />)}
      </Table.DataCell>
    </Table.ExpandableRow>
  );
}
