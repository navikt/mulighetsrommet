import { Alert, Button, Link, Table } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import {
  IndividuellGjennomforing,
  useIndividuelleGjennomforinger,
} from "@/api/individuell-gjennomforing/useIndividuelleGjennomforinger";
import { PlusIcon } from "@navikt/aksel-icons";
import { useNavigate } from "react-router";
import { IndividuellGjennomforingFilterType } from "@/pages/individuell-gjennomforing/filter";

interface Props {
  filter: IndividuellGjennomforingFilterType;
}

export function IndividuellGjennomforingTabell({ filter }: Props) {
  const navigate = useNavigate();
  const { data: gjennomforinger } = useIndividuelleGjennomforinger(filter);

  return (
    <>
      <div className="flex justify-end mb-4">
        <Button
          size="small"
          icon={<PlusIcon aria-hidden />}
          onClick={() => navigate("/individuelle-gjennomforinger/opprett")}
        >
          Opprett individuell gjennomføring
        </Button>
      </div>
      {gjennomforinger.length === 0 ? (
        <Alert variant="info">Fant ingen individuelle gjennomføringer</Alert>
      ) : (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader>Navn</Table.ColumnHeader>
              <Table.ColumnHeader>Tiltakstype</Table.ColumnHeader>
              <Table.ColumnHeader>Arrangør</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {gjennomforinger.map((gjennomforing: IndividuellGjennomforing) => (
              <Table.Row key={gjennomforing.id}>
                <Table.DataCell>
                  <Link
                    as={ReactRouterLink}
                    to={`/individuelle-gjennomforinger/${gjennomforing.id}`}
                  >
                    {gjennomforing.navn}
                  </Link>
                </Table.DataCell>
                <Table.DataCell>{gjennomforing.tiltakstype?.navn ?? "—"}</Table.DataCell>
                <Table.DataCell>{gjennomforing.arrangor?.navn ?? "—"}</Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}
