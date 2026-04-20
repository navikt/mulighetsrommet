import { useTilskuddBehandlinger } from "@/api/tilskudd-behandling/useTilskuddBehandlinger";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { TilskuddBehandlingStatus } from "@tiltaksadministrasjon/api-client";
import { ActionMenu, Alert, Table, Tag } from "@navikt/ds-react";
import { Link, useNavigate } from "react-router";

function statusTag(status: TilskuddBehandlingStatus) {
  switch (status) {
    case TilskuddBehandlingStatus.TIL_GODKJENNING:
      return (
        <Tag variant="warning" size="small">
          Til godkjenning
        </Tag>
      );
    case TilskuddBehandlingStatus.GODKJENT:
      return (
        <Tag variant="success" size="small">
          Godkjent
        </Tag>
      );
    case TilskuddBehandlingStatus.RETURNERT:
      return (
        <Tag variant="neutral" size="small">
          Returnert
        </Tag>
      );
  }
}

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
                <Table.DataCell>{statusTag(b.status)}</Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}
