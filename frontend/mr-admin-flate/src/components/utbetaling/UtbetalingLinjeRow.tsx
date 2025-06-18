import { delutbetalingAarsakTilTekst, formaterPeriode, tilsagnTypeToString } from "@/utils/Utils";
import { DelutbetalingReturnertAarsak, FieldError, UtbetalingLinje } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  BodyShort,
  Checkbox,
  Heading,
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
import { TilsagnInformasjon } from "./TilsagnInformasjon";
import { DelutbetalingTag } from "./DelutbetalingTag";
import { BehandlerInformasjon } from "./BehandlerInformasjon";

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
    if (skalApneRad) {
      setOpenRow(skalApneRad);
    }
  }, [skalApneRad]);

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
        <VStack gap="4">
          {linje.opprettelse?.type === "BESLUTTET" && linje.opprettelse.besluttelse === "AVVIST" ? (
            <VStack>
              {linje.opprettelse.aarsaker.includes(DelutbetalingReturnertAarsak.PROPAGERT_RETUR) ? (
                <Alert size="medium" variant="warning">
                  <Heading size="small" level="4">
                    Automatisk returnert som følge av at en annen utbetalingslinje ble returnert
                  </Heading>
                </Alert>
              ) : (
                <AarsakerOgForklaring
                  heading="Linjen ble returnert på grunn av følgende årsaker:"
                  aarsaker={linje.opprettelse.aarsaker.map((aarsak) =>
                    delutbetalingAarsakTilTekst(aarsak as DelutbetalingReturnertAarsak),
                  )}
                  forklaring={linje.opprettelse.forklaring}
                />
              )}
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
          <HStack gap="4" justify="space-between">
            <TilsagnInformasjon tilsagn={linje.tilsagn} />
            {linje.opprettelse && (
              <BehandlerInformasjon opprettelse={linje.opprettelse} status={linje.status} />
            )}
          </HStack>
        </VStack>
      }
    >
      <Table.HeaderCell className={grayBgClass}>
        <Link to={`/gjennomforinger/${gjennomforingId}/tilsagn/${linje.tilsagn.id}`}>
          {linje.tilsagn.bestillingsnummer}
        </Link>
      </Table.HeaderCell>
      <Table.DataCell className={grayBgClass}>
        {tilsagnTypeToString(linje.tilsagn.type)}
      </Table.DataCell>
      <Table.DataCell className={grayBgClass}>
        {formaterPeriode(linje.tilsagn.periode)}
      </Table.DataCell>
      <Table.DataCell className={grayBgClass}>{linje.tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell className={grayBgClass}>
        {formaterNOK(linje.tilsagn.belopGjenstaende)}
      </Table.DataCell>
      <Table.DataCell>
        <HStack gap="2">
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
      <Table.DataCell>
        <TextField
          size="small"
          style={{ maxWidth: "6rem" }}
          error={belopError}
          label="Utbetales"
          readOnly={readOnly}
          hideLabel
          inputMode="numeric"
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
      <Table.DataCell align="right">{knappeColumn}</Table.DataCell>
    </Table.ExpandableRow>
  );
}
