import {
  Alert,
  BodyShort,
  Box,
  Button,
  HGrid,
  HStack,
  List,
  Skeleton,
  Table,
  VStack,
} from "@navikt/ds-react";
import { TiltakDeltMedBruker } from "@mr/api-client";
import { ReactNode } from "react";
import { formaterDato } from "../../../utils/Utils";
import { ModiaRoute, navigateToModiaApp } from "../ModiaRoute";
import { useDeltMedBrukerHistorikk } from "../hooks/useDeltMedBrukerHistorikk";
import { IngenFunnetBox } from "../views/Landingsside";
import { VisningsnavnForTiltak } from "../../../components/oversikt/VisningsnavnForTiltak";

function sortOnCreatedAt(a: TiltakDeltMedBruker, b: TiltakDeltMedBruker) {
  return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
}

export function DelMedBrukerHistorikk() {
  const { data = [], isLoading, error } = useDeltMedBrukerHistorikk();

  if (error) {
    return <Alert variant="error">Kunne ikke hente Delt i dialogen-historikk</Alert>;
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
    return <IngenFunnetBox title="Det er ikke delt informasjon om noen tiltak med brukeren" />;
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
    <Box padding="2" background="bg-default">
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
    </Box>
  );
}

function NavigateToDialogButton({ tiltak }: { tiltak: TiltakDeltMedBruker }): ReactNode {
  return (
    <Button
      as="a"
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
  const tidligereDelte = delteTiltak.slice(1);

  return (
    <List title="Tidligere delinger">
      {tidligereDelte.map((delt) => {
        return (
          <List.Item key={delt.dialogId}>
            <HStack gap="5" align="start">
              <VisningsnavnForTiltak
                noLink
                navn={delt.lokaltNavn}
                tiltakstype={{
                  tiltakskode: delt.tiltakstype.tiltakskode,
                  arenakode: delt.tiltakstype?.arenakode,
                  navn: delt.tiltakstype.navn,
                }}
              />
              <BodyShort size="small">Delt {formaterDato(delt.createdAt)}</BodyShort>
            </HStack>
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
      <Table.DataCell>
        <VisningsnavnForTiltak
          noLink
          navn={tiltak.lokaltNavn}
          tiltakstype={{
            tiltakskode: tiltak.tiltakstype.tiltakskode,
            arenakode: tiltak.tiltakstype?.arenakode,
            navn: tiltak.tiltakstype.navn,
          }}
        />
      </Table.DataCell>
      <Table.DataCell title={tiltak.createdAt}>{formaterDato(tiltak.createdAt)}</Table.DataCell>
      <Table.DataCell>
        <NavigateToDialogButton tiltak={tiltak} />
      </Table.DataCell>
    </>
  );
}
