import { Alert, Button, Link, Table } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { useTiltakDokumenter } from "@/api/tiltak-dokument/useTiltakDokumenter";
import { PlusIcon } from "@navikt/aksel-icons";
import { useNavigate } from "react-router";
import { TiltakDokumentFilterType } from "@/pages/tiltak-dokument/filter";
import { TiltakDokumentKompaktDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  filter: TiltakDokumentFilterType;
}

export function TiltakDokumentTabell({ filter }: Props) {
  const navigate = useNavigate();
  const { data: gjennomforinger } = useTiltakDokumenter(filter);

  return (
    <>
      <div className="flex justify-end mb-4">
        <Button
          size="small"
          icon={<PlusIcon aria-hidden />}
          onClick={() => navigate("/tiltak-dokumenter/opprett")}
        >
          Opprett tiltaksdokument
        </Button>
      </div>
      {gjennomforinger.length === 0 ? (
        <Alert variant="info">Fant ingen tiltaksdokumenter</Alert>
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
            {gjennomforinger.map((gjennomforing: TiltakDokumentKompaktDto) => (
              <Table.Row key={gjennomforing.id}>
                <Table.DataCell>
                  <Link as={ReactRouterLink} to={`/tiltak-dokumenter/${gjennomforing.id}`}>
                    {gjennomforing.navn}
                  </Link>
                </Table.DataCell>
                <Table.DataCell>{gjennomforing.tiltakstype.navn}</Table.DataCell>
                <Table.DataCell>{gjennomforing.arrangor?.navn ?? "-"}</Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}
