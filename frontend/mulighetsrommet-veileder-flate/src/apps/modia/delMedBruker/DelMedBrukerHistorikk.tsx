import { TiltakDeltMedBrukerDto } from "@api-client";
import {
  BodyShort,
  Box,
  Button,
  HStack,
  List,
  Table,
  VStack,
  Heading,
  Link,
} from "@navikt/ds-react";
import { ReactNode } from "react";
import { Link as ReactRouterLink } from "react-router";
import { VisningsnavnForTiltak } from "@/components/oversikt/VisningsnavnForTiltak";
import { formaterDato } from "@/utils/Utils";
import { ModiaRoute, navigateToModiaApp } from "../ModiaRoute";
import { useDeltMedBrukerHistorikk } from "../hooks/useDeltMedBrukerHistorikk";
import { IngenFunnetBox } from "../views/Landingsside";

function sortOnCreatedAt(a: TiltakDeltMedBrukerDto, b: TiltakDeltMedBrukerDto) {
  return new Date(b.deling.tidspunkt).getTime() - new Date(a.deling.tidspunkt).getTime();
}

export function DelMedBrukerHistorikk() {
  const { data = [] } = useDeltMedBrukerHistorikk();

  if (data.length === 0) {
    return <IngenFunnetBox title="Det er ikke delt informasjon om noen tiltak med brukeren" />;
  }

  const gruppertHistorikk = data
    .sort(sortOnCreatedAt)
    .reduce<Record<string, TiltakDeltMedBrukerDto[]>>((acc, obj) => {
      (acc[obj.tiltak.id] ??= []).push(obj);
      return acc;
    }, {});

  // Sort each group by createdAt
  Object.keys(gruppertHistorikk).forEach((tiltakId) => {
    gruppertHistorikk[tiltakId].sort(sortOnCreatedAt);
  });

  return (
    <Box padding="space-8" background="default">
      <VStack gap="space-8">
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

function contentForRow(delinger: TiltakDeltMedBrukerDto[]): ReactNode {
  const tidligereDelinger = delinger.slice(1);

  return (
    <>
      <Heading as="h3" size="small">
        Tidligere delinger
      </Heading>
      <Box marginBlock="space-16" asChild>
        <List data-aksel-migrated-v8>
          {tidligereDelinger.map(({ deling, tiltak, tiltakstype }) => {
            return (
              <List.Item key={deling.dialogId}>
                <HStack gap="space-20" align="start">
                  <VisningsnavnForTiltak
                    noLink
                    tiltakstypeNavn={tiltakstype.navn}
                    navn={tiltak.navn}
                  />
                  <BodyShort size="small">Delt {formaterDato(deling.tidspunkt)}</BodyShort>
                </HStack>
              </List.Item>
            );
          })}
        </List>
      </Box>
    </>
  );
}

function createCells(antallTiltakDelt: number, deltMedBruker: TiltakDeltMedBrukerDto): ReactNode {
  return (
    <>
      {antallTiltakDelt === 1 ? <Table.DataCell></Table.DataCell> : null}
      <Table.DataCell>
        <VisningsnavnForTiltak
          noLink
          tiltakstypeNavn={deltMedBruker.tiltakstype.navn}
          navn={deltMedBruker.tiltak.navn}
        />
      </Table.DataCell>
      <Table.DataCell>{formaterDato(deltMedBruker.deling.tidspunkt)}</Table.DataCell>
      <Table.DataCell>
        <VStack align="center" gap="space-12">
          <Button
            variant="secondary"
            size="small"
            onClick={(e) => {
              e.preventDefault();
              navigateToModiaApp({
                route: ModiaRoute.DIALOG,
                dialogId: deltMedBruker.deling.dialogId,
              });
            }}
          >
            Gå til dialogen
          </Button>
          <Link as={ReactRouterLink} to={`/arbeidsmarkedstiltak/tiltak/${deltMedBruker.tiltak.id}`}>
            <BodyShort size="small">Gå til tiltak</BodyShort>
          </Link>
        </VStack>
      </Table.DataCell>
    </>
  );
}
