import { useTilskuddBehandlinger } from "@/api/tilskudd-behandling/useTilskuddBehandlinger";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ActionMenu, Table } from "@navikt/ds-react";
import { Link } from "react-router";

export function TilskuddBehandlingerPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: behandlinger } = useTilskuddBehandlinger(gjennomforingId);

  return (
    <>
      <KnapperadContainer>
        <Handlinger>
          <ActionMenu.Item as={Link} to={`opprett`}>
            Opprett tilskuddsbehandling
          </ActionMenu.Item>
        </Handlinger>
      </KnapperadContainer>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Søknadsdato</Table.HeaderCell>
            <Table.HeaderCell>Periode</Table.HeaderCell>
            <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {behandlinger.map((b) => (
            <Table.Row key={b.id}>
              <Table.DataCell>{b.soknadDato}</Table.DataCell>
              <Table.DataCell>
                {b.periode.start} – {b.periode.slutt}
              </Table.DataCell>
              <Table.DataCell>{b.kostnadssted}</Table.DataCell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table>
    </>
  );
}
