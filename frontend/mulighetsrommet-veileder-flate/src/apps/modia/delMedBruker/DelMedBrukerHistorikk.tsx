import { Alert, Button, HGrid, List, Skeleton, Table, VStack } from "@navikt/ds-react";
import { TiltakDeltMedBruker } from "mulighetsrommet-api-client";
import { ReactNode } from "react";
import { formaterDato } from "../../../utils/Utils";
import { ModiaRoute, navigateToModiaApp } from "../ModiaRoute";
import { useDeltMedBrukerHistorikk } from "../hooks/useDeltMedBrukerHistorikk";

function sortOnCreatedAt(a: TiltakDeltMedBruker, b: TiltakDeltMedBruker) {
  return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
}

export function DelMedBrukerHistorikk() {
  const { data = [], isLoading, error } = useDeltMedBrukerHistorikk();

  if (error) {
    return <Alert variant="error">Kunne ikke hente Delt med bruker-historikk</Alert>;
  }

  if (isLoading) {
    return (
      <VStack gap="2">
        <Skeleton width="100%" height="3rem" />
        <Skeleton width="100%" height="3rem" />
        <Skeleton width="100%" height="3rem" />
      </VStack>
    );
  }

  if (data.length === 0) {
    return <Alert variant="info">Det er ikke delt informasjon om noen tiltak med brukeren</Alert>;
  }

  const gruppertHistorikk = data.sort(sortOnCreatedAt).reduce(
    (acc, obj) => {
      // If the tiltakId key doesn't exist, create it with an empty array
      if (!acc[obj.tiltakId]) {
        acc[obj.tiltakId] = [];
      }
      // Push the current object to the array for its tiltakId
      acc[obj.tiltakId].push(obj);
      return acc;
    },
    {} as Record<string, TiltakDeltMedBruker[]>,
  );

  // Sort each group by createdAt
  Object.keys(gruppertHistorikk).forEach((tiltakId) => {
    gruppertHistorikk[tiltakId].sort(sortOnCreatedAt);
  });

  return (
    <VStack gap="2">
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Tiltak</Table.HeaderCell>
            <Table.HeaderCell scope="col">Delt</Table.HeaderCell>
            <Table.HeaderCell scope="col"></Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {Object.keys(gruppertHistorikk).map((tiltakId) => {
            const delteTiltak = gruppertHistorikk[tiltakId];
            const sisteDelt = delteTiltak[0];

            if (delteTiltak.length === 1) {
              return (
                <Table.Row key={tiltakId}>{createCells(delteTiltak.length, sisteDelt)}</Table.Row>
              );
            } else {
              return (
                <Table.ExpandableRow
                  expandOnRowClick
                  key={tiltakId}
                  content={contentForRow(delteTiltak)}
                >
                  {createCells(delteTiltak.length, sisteDelt)}
                </Table.ExpandableRow>
              );
            }
          })}
        </Table.Body>
      </Table>
    </VStack>
  );
}

function navigateToDialogButton(tiltak: TiltakDeltMedBruker): ReactNode {
  return (
    <Button
      style={{ textDecoration: "underline", margin: 0, padding: 0, color: "#0067c5" }}
      variant="tertiary-neutral"
      onClick={(e) => {
        e.preventDefault();
        navigateToModiaApp({
          route: ModiaRoute.DIALOG,
          dialogId: tiltak.dialogId,
        });
      }}
    >
      Gå til dialogen
    </Button>
  );
}

function contentForRow(delteTiltak: TiltakDeltMedBruker[]): ReactNode {
  return (
    <List>
      {delteTiltak.map((delt) => {
        return (
          <List.Item key={delt.dialogId}>
            <HGrid columns={2} gap="2" align="start">
              <div>
                {delt.navn} - {formaterDato(delt.createdAt)}
              </div>
              <div>{navigateToDialogButton(delt)}</div>
            </HGrid>
          </List.Item>
        );
      })}
    </List>
  );
}

function createCells(antallTiltakDelt: number, tiltak: TiltakDeltMedBruker): ReactNode {
  return (
    <>
      {antallTiltakDelt === 1 ? <Table.DataCell></Table.DataCell> : null}
      <Table.DataCell>{tiltak.navn}</Table.DataCell>
      <Table.DataCell title={tiltak.createdAt}>{formaterDato(tiltak.createdAt)}</Table.DataCell>
      <Table.DataCell>{navigateToDialogButton(tiltak)}</Table.DataCell>
    </>
  );
}
