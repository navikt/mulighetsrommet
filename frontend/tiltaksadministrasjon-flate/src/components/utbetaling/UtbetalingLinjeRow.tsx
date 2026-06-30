import {
  tilsagnTypeToString,
  utbetalingLinjeAarsakTilTekst,
  ValidationMessage,
} from "@/utils/Utils";
import {
  UtbetalingLinjeDto,
  UtbetalingLinjeReturnertAarsak,
} from "@tiltaksadministrasjon/api-client";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { Alert, Heading, HStack, InfoCard, Link, List, Table, VStack } from "@navikt/ds-react";
import React, { useState } from "react";
import { Link as ReactRouterLink } from "react-router";
import { AarsakerOgForklaring } from "@/pages/gjennomforing/tilsagn/AarsakerOgForklaring";
import { TilsagnInformasjon } from "./TilsagnInformasjon";
import { UtbetalingLinjeStatusTag } from "./UtbetalingLinjeStatusTag";
import { BehandlerInformasjon } from "./BehandlerInformasjon";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { erReturnert, erBesluttet } from "@/utils/totrinnskontroll";

interface Props {
  gjennomforingId: string;
  linje: UtbetalingLinjeDto;
  belopInput?: React.ReactNode | null;
  checkboxInput?: React.ReactNode | null;
  knappeColumn?: React.ReactNode;
  errors?: ValidationMessage[];
  onChange?: (linje: UtbetalingLinjeDto) => void;
}

export function UtbetalingLinjeRow({
  gjennomforingId,
  linje,
  knappeColumn,
  belopInput = null,
  checkboxInput = null,
  errors = [],
}: Props) {
  const [opened, setOpened] = useState(erBesluttet(linje.opprettelse) || errors.length > 0);

  const openRow = opened || erBesluttet(linje.opprettelse) || errors.length > 0;

  return (
    <Table.ExpandableRow
      shadeOnHover={false}
      open={openRow}
      onOpenChange={() => setOpened(!opened)}
      key={`${linje.id}-${linje.status?.type}`}
      className={`[&>td:nth-of-type(-n+5)]:bg-ax-neutral-200`}
      content={
        <VStack gap="space-16">
          {erReturnert(linje.opprettelse) ? (
            <VStack>
              {linje.opprettelse.aarsaker.includes(
                UtbetalingLinjeReturnertAarsak.PROPAGERT_RETUR,
              ) ? (
                <Alert size="medium" variant="warning">
                  <Heading size="small" level="4">
                    Automatisk returnert som følge av at en annen utbetalingslinje ble returnert
                  </Heading>
                </Alert>
              ) : (
                <AarsakerOgForklaring
                  heading="Linjen ble returnert på grunn av følgende årsaker"
                  aarsaker={linje.opprettelse.aarsaker.map((aarsak) =>
                    utbetalingLinjeAarsakTilTekst(aarsak as UtbetalingLinjeReturnertAarsak),
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
            <TilsagnInformasjon tilsagn={linje.tilsagn} deltakere={linje.deltakere} />
            {linje.opprettelse && <BehandlerInformasjon opprettelse={linje.opprettelse} />}
          </HStack>
        </VStack>
      }
    >
      <Table.HeaderCell className="bg-ax-neutral-200">
        <Link
          as={ReactRouterLink}
          to={`/gjennomforinger/${gjennomforingId}/tilsagn/${linje.tilsagn.id}`}
        >
          {linje.tilsagn.bestillingsnummer}
        </Link>
      </Table.HeaderCell>
      <Table.DataCell>{tilsagnTypeToString(linje.tilsagn.type)}</Table.DataCell>
      <Table.DataCell>{formaterPeriode(linje.tilsagn.periode)}</Table.DataCell>
      <Table.DataCell>{linje.tilsagn.kostnadssted.navn}</Table.DataCell>
      <Table.DataCell>
        {formaterValuta(
          linje.tilsagn.belopGjenstaende.belop,
          linje.tilsagn.belopGjenstaende.valuta,
        )}
      </Table.DataCell>
      <Table.DataCell>{checkboxInput}</Table.DataCell>
      <Table.DataCell>{belopInput}</Table.DataCell>
      <Table.DataCell>
        {linje.status && <UtbetalingLinjeStatusTag status={linje.status} />}
      </Table.DataCell>
      <Table.DataCell>{knappeColumn}</Table.DataCell>
    </Table.ExpandableRow>
  );
}
