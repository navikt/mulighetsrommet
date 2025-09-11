import { delutbetalingAarsakTilTekst, tilsagnTypeToString } from "@/utils/Utils";
import { FieldError } from "@mr/api-client-v2";
import { DelutbetalingReturnertAarsak, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Alert, BodyShort, Heading, HStack, List, Table, VStack } from "@navikt/ds-react";
import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { AarsakerOgForklaring } from "@/pages/gjennomforing/tilsagn/AarsakerOgForklaring";
import { TilsagnInformasjon } from "./TilsagnInformasjon";
import { DelutbetalingStatusTag } from "./DelutbetalingStatusTag";
import { BehandlerInformasjon } from "./BehandlerInformasjon";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { isBesluttet } from "@/utils/totrinnskontroll";

interface Props {
  linje: UtbetalingLinje;
  textInput?: React.ReactNode | null;
  checkboxInput?: React.ReactNode | null;
  knappeColumn?: React.ReactNode;
  onChange?: (linje: UtbetalingLinje) => void;
  errors?: FieldError[];
  grayBackground?: boolean;
  rowOpen?: boolean;
}

export function UtbetalingLinjeRow({
  linje,
  errors = [],
  knappeColumn,
  textInput = null,
  checkboxInput = null,
  grayBackground = false,
  rowOpen = false,
}: Props) {
  const { gjennomforingId } = useParams();
  const [belopError, setBelopError] = useState<string | undefined>();
  const [openRow, setOpenRow] = useState(rowOpen);
  const grayBgClass = grayBackground ? "bg-gray-100" : "";

  useEffect(() => {
    if (rowOpen) {
      setOpenRow(rowOpen);
    }
  }, [rowOpen]);

  useEffect(() => {
    setBelopError(errors.find((e) => e.pointer.includes("belop"))?.detail);
  }, [belopError, errors]);

  return (
    <Table.ExpandableRow
      shadeOnHover={false}
      open={openRow}
      onOpenChange={() => setOpenRow(!openRow)}
      key={`${linje.id}-${linje.status?.type}`}
      className={`${grayBackground ? "[&>td:first-child]:bg-gray-100" : ""}`}
      content={
        <VStack gap="4">
          {isBesluttet(linje.opprettelse) && linje.opprettelse.besluttelse === "AVVIST" ? (
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
            {linje.opprettelse && <BehandlerInformasjon opprettelse={linje.opprettelse} />}
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
      <Table.DataCell>{checkboxInput}</Table.DataCell>
      <Table.DataCell>{textInput}</Table.DataCell>
      <Table.DataCell>
        {linje.status && <DelutbetalingStatusTag status={linje.status} />}
      </Table.DataCell>
      <Table.DataCell align="right">{knappeColumn}</Table.DataCell>
    </Table.ExpandableRow>
  );
}
