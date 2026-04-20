import { useTilskuddBehandlinger } from "@/api/tilskudd-behandling/useTilskuddBehandlinger";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { DataElementStatusTag } from "@mr/frontend-common";
import { ActionMenu, Alert, Table } from "@navikt/ds-react";
import { Link, useNavigate } from "react-router";

export function TilskuddBehandlingerPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: behandlinger } = useTilskuddBehandlinger(gjennomforingId);
  const navigate = useNavigate();

  return (
    <>
      <KnapperadContainer>
        <Handlinger>
          <ActionMenu.Item as={Link} to={`opprett`}>
            Opprett tilskuddsbehandling
          </ActionMenu.Item>
        </Handlinger>
      </KnapperadContainer>
      {behandlinger.length === 0 && (
        <Alert variant="info" className="mt-4">
          Det finnes ingen tilskuddsbehandlinger for dette tiltaket
        </Alert>
      )}
      {behandlinger.length > 0 && (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Søknadsdato</Table.HeaderCell>
              <Table.HeaderCell>Periode</Table.HeaderCell>
              <Table.HeaderCell>Kostnadssted</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {behandlinger.map((b) => (
              <Table.Row onClick={() => navigate(b.id)} key={b.id}>
                <Table.DataCell>
                  <Link to={b.id}>{b.soknadDato}</Link>
                </Table.DataCell>
                <Table.DataCell>
                  {b.periode.start} – {b.periode.slutt}
                </Table.DataCell>
                <Table.DataCell>{b.kostnadssted}</Table.DataCell>
                <Table.DataCell>
                  <DataElementStatusTag {...b.status.status} />
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}
