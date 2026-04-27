import { useTilskuddBehandlinger } from "@/api/tilskudd-behandling/useTilskuddBehandlinger";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { DataElementStatusTag } from "@mr/frontend-common";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { ActionMenu, Alert, Table } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { formaterDato } from "@mr/frontend-common/utils/date";

export function TilskuddBehandlingerPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: behandlinger } = useTilskuddBehandlinger(gjennomforingId);
  const navigate = useNavigate();

  return (
    <>
      <KnapperadContainer>
        <Handlinger>
          <ActionMenu.Item onClick={() => navigate("opprett")}>
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
              <Table.HeaderCell>Innsendt</Table.HeaderCell>
              <Table.HeaderCell>JournalpostId</Table.HeaderCell>
              <Table.HeaderCell>Periodestart</Table.HeaderCell>
              <Table.HeaderCell>Periodeslutt</Table.HeaderCell>
              <Table.HeaderCell>Tilskuddstype</Table.HeaderCell>
              <Table.HeaderCell>Behandlingsstatus</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {behandlinger.map((b) => (
              <Table.Row key={b.id}>
                <Table.DataCell>{formaterDato(b.soknadDato)}</Table.DataCell>
                <Table.DataCell>{b.journalpostId}</Table.DataCell>
                <Table.DataCell>{formaterDato(b.periode.start)}</Table.DataCell>
                <Table.DataCell>{formaterDato(b.periode.slutt)}</Table.DataCell>
                <Table.DataCell>{b.tilskuddtyper.join(", ")}</Table.DataCell>
                <Table.DataCell>
                  <DataElementStatusTag {...b.status.status} />
                </Table.DataCell>
                <Table.DataCell>
                  <Lenke to={b.id}> Detaljer </Lenke>
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}
