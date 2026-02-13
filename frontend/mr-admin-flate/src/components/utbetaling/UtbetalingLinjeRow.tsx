import { delutbetalingAarsakTilTekst, tilsagnTypeToString } from "@/utils/Utils";
import {
  DelutbetalingReturnertAarsak,
  FieldError,
  UtbetalingLinje,
} from "@tiltaksadministrasjon/api-client";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import {
  Alert,
  BodyShort,
  Heading,
  HStack,
  List,
  Table,
  VStack,
  Box,
  Link,
} from "@navikt/ds-react";
import React, { useEffect, useState } from "react";
import { Link as ReactRouterLink } from "react-router";
import { AarsakerOgForklaring } from "@/pages/gjennomforing/tilsagn/AarsakerOgForklaring";
import { TilsagnInformasjon } from "./TilsagnInformasjon";
import { DelutbetalingStatusTag } from "./DelutbetalingStatusTag";
import { BehandlerInformasjon } from "./BehandlerInformasjon";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { isBesluttet } from "@/utils/totrinnskontroll";

interface Props {
  gjennomforingId: string;
  linje: UtbetalingLinje;
  belopInput?: React.ReactNode | null;
  checkboxInput?: React.ReactNode | null;
  knappeColumn?: React.ReactNode;
  onChange?: (linje: UtbetalingLinje) => void;
  errors?: FieldError[];
  grayBackground?: boolean;
  rowOpen?: boolean;
}

export function UtbetalingLinjeRow({
  gjennomforingId,
  linje,
  errors = [],
  knappeColumn,
  belopInput = null,
  checkboxInput = null,
  grayBackground = false,
  rowOpen = false,
}: Props) {
  const [belopError, setBelopError] = useState<string | undefined>();
  const [openRow, setOpenRow] = useState(rowOpen);
  const grayBgClass = grayBackground ? "bg-ax-neutral-200" : "";

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
      className={`${grayBackground ? "[&>td:first-child]:bg-ax-neutral-200" : ""}`}
      content={
        <VStack gap="space-16">
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
            <VStack className="bg-[var(--ax-bg-danger-soft)]">
              <Alert size="small" variant="error">
                <BodyShort>Følgende feil må fikses:</BodyShort>
                <Box marginBlock="space-16" asChild>
                  <List data-aksel-migrated-v8>
                    {errors.map((error) => (
                      <List.Item>{error.detail}</List.Item>
                    ))}
                  </List>
                </Box>
              </Alert>
            </VStack>
          )}
          <HStack gap="space-16" justify="space-between">
            <TilsagnInformasjon tilsagn={linje.tilsagn} />
            {linje.opprettelse && <BehandlerInformasjon opprettelse={linje.opprettelse} />}
          </HStack>
        </VStack>
      }
    >
      <Table.HeaderCell className={grayBgClass}>
        <Link
          as={ReactRouterLink}
          to={`/gjennomforinger/${gjennomforingId}/tilsagn/${linje.tilsagn.id}`}
        >
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
        {formaterValuta(
          linje.tilsagn.belopGjenstaende.belop,
          linje.tilsagn.belopGjenstaende.valuta,
        )}
      </Table.DataCell>
      <Table.DataCell>{checkboxInput}</Table.DataCell>
      <Table.DataCell>{belopInput}</Table.DataCell>
      <Table.DataCell>
        {linje.status && <DelutbetalingStatusTag status={linje.status} />}
      </Table.DataCell>
      <Table.DataCell align="right">{knappeColumn}</Table.DataCell>
    </Table.ExpandableRow>
  );
}
