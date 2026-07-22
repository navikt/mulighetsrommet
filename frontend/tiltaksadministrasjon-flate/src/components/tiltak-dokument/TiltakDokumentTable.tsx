import { Alert, Button, Link, Table, Tag, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { useTiltakDokumenter } from "@/api/tiltak-dokument/useTiltakDokumenter";
import { PlusIcon } from "@navikt/aksel-icons";
import { useNavigate } from "react-router";
import { TiltakDokumentFilterType } from "@/pages/tiltak-dokument/filter";
import { TiltakDokumentKompaktDto } from "@tiltaksadministrasjon/api-client";
import { formaterNavEnheter } from "@/utils/Utils";

interface Props {
  filter: TiltakDokumentFilterType;
}

export function TiltakDokumentTable({ filter }: Props) {
  const navigate = useNavigate();
  const { data: tiltakDokumenter } = useTiltakDokumenter(filter);

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
      {tiltakDokumenter.length === 0 ? (
        <Alert variant="info">Fant ingen tiltaksdokumenter</Alert>
      ) : (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader>Navn</Table.ColumnHeader>
              <Table.ColumnHeader>Tiltakstype</Table.ColumnHeader>
              <Table.ColumnHeader>Enhet</Table.ColumnHeader>
              <Table.ColumnHeader>Arrangør</Table.ColumnHeader>
              <Table.ColumnHeader />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {tiltakDokumenter.map((gjennomforing: TiltakDokumentKompaktDto) => (
              <Table.Row key={gjennomforing.id}>
                <Table.DataCell>
                  <Link as={ReactRouterLink} to={`/tiltak-dokumenter/${gjennomforing.id}`}>
                    {gjennomforing.navn}
                  </Link>
                </Table.DataCell>
                <Table.DataCell>{gjennomforing.tiltakstype.navn}</Table.DataCell>
                <Table.DataCell
                  aria-label={`Enheter: ${gjennomforing.kontorstruktur
                    .map((struktur) => struktur.kontorer.map((kontor) => kontor.navn))
                    .join(", ")}`}
                  title={`Enheter: ${gjennomforing.kontorstruktur
                    .map((struktur) => struktur.kontorer.map((kontor) => kontor.navn))
                    .join(", ")}`}
                >
                  {formaterNavEnheter(
                    gjennomforing.kontorstruktur.flatMap((struktur) =>
                      struktur.kontorer.map((kontor) => ({
                        navn: kontor.navn,
                        enhetsnummer: kontor.enhetsnummer,
                      })),
                    ),
                  )}
                </Table.DataCell>
                <Table.DataCell>{gjennomforing.arrangor?.navn ?? "-"}</Table.DataCell>
                <Table.DataCell>
                  <VStack align="center">
                    {gjennomforing.publisert && (
                      <Tag
                        data-color="success"
                        title="Tiltaksdokumentet er publisert og synlig for veileder i Modia"
                        variant="strong"
                        size="small"
                      >
                        Publisert
                      </Tag>
                    )}
                  </VStack>
                </Table.DataCell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </>
  );
}
