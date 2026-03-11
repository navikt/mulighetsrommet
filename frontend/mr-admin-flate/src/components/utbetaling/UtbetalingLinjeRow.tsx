import { delutbetalingAarsakTilTekst, tilsagnTypeToString, ValidationMessage } from "@/utils/Utils";
import { DelutbetalingReturnertAarsak, UtbetalingLinje } from "@tiltaksadministrasjon/api-client";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { Alert, Heading, HStack, List, Table, VStack, Link, InfoCard } from "@navikt/ds-react";
import React, { useState } from "react";
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
  errors?: ValidationMessage[];
  onChange?: (linje: UtbetalingLinje) => void;
}

export function UtbetalingLinjeRow({
  gjennomforingId,
  linje,
  knappeColumn,
  belopInput = null,
  checkboxInput = null,
  errors = [],
}: Props) {
  const [opened, setOpened] = useState(isBesluttet(linje.opprettelse) || errors.length > 0);
  const grayBgClass = "bg-ax-neutral-200";

  const openRow = opened || isBesluttet(linje.opprettelse) || errors.length > 0;

  return (
    <Table.ExpandableRow
      shadeOnHover={false}
      open={openRow}
      onOpenChange={() => setOpened(!opened)}
      key={`${linje.id}-${linje.status?.type}`}
      className={`[&>td:first-child]:${grayBgClass}`}
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
                  heading="Linjen ble returnert på grunn av følgende årsaker"
                  aarsaker={linje.opprettelse.aarsaker.map((aarsak) =>
                    delutbetalingAarsakTilTekst(aarsak as DelutbetalingReturnertAarsak),
                  )}
                  forklaring={linje.opprettelse.forklaring}
                />
              )}
            </VStack>
          ) : null}
          {errors.length > 0 && (
            <InfoCard size="small" data-color="danger">
              <InfoCard.Header>
                <InfoCard.Title>Følgende feil må fikses:</InfoCard.Title>
              </InfoCard.Header>
              <InfoCard.Content>
                <List>
                  {errors.map((error, index) => (
                    <List.Item key={index}>{error.message}</List.Item>
                  ))}
                </List>
              </InfoCard.Content>
            </InfoCard>
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
