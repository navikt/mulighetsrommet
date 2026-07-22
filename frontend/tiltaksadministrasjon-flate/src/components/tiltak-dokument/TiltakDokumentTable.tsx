import { Alert, Button, Link, Pagination, Table, Tag, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { useTiltakDokumenter } from "@/api/tiltak-dokument/useTiltakDokumenter";
import { PlusIcon } from "@navikt/aksel-icons";
import { useNavigate } from "react-router";
import { TiltakDokumentFilterType } from "@/pages/tiltak-dokument/filter";
import { TiltakDokumentKompaktDto } from "@tiltaksadministrasjon/api-client";
import { formaterNavEnheter } from "@/utils/Utils";
import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { PagineringsOversikt } from "@/components/paginering/PagineringOversikt";
import { PagineringContainer } from "@/components/paginering/PagineringContainer";

interface Props {
  filter: TiltakDokumentFilterType;
  updateFilter: (values: Partial<TiltakDokumentFilterType>) => void;
}

export function TiltakDokumentTable({ filter, updateFilter }: Props) {
  const navigate = useNavigate();
  const {
    data: { pagination, data: tiltakDokumenter },
  } = useTiltakDokumenter(filter);

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
      <TabellWrapper>
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
              {tiltakDokumenter.map((dokument: TiltakDokumentKompaktDto) => (
                <Table.Row key={dokument.id}>
                  <Table.DataCell>
                    <Link as={ReactRouterLink} to={`/tiltak-dokumenter/${dokument.id}`}>
                      {dokument.navn}
                    </Link>
                  </Table.DataCell>
                  <Table.DataCell>{dokument.tiltakstype.navn}</Table.DataCell>
                  <Table.DataCell
                    aria-label={`Enheter: ${dokument.kontorstruktur
                      .map((struktur) => struktur.kontorer.map((kontor) => kontor.navn))
                      .join(", ")}`}
                    title={`Enheter: ${dokument.kontorstruktur
                      .map((struktur) => struktur.kontorer.map((kontor) => kontor.navn))
                      .join(", ")}`}
                  >
                    {formaterNavEnheter(
                      dokument.kontorstruktur.flatMap((struktur) =>
                        struktur.kontorer.map((kontor) => ({
                          navn: kontor.navn,
                          enhetsnummer: kontor.enhetsnummer,
                        })),
                      ),
                    )}
                  </Table.DataCell>
                  <Table.DataCell>{dokument.arrangor?.navn ?? "-"}</Table.DataCell>
                  <Table.DataCell>
                    <VStack align="center">
                      {dokument.publisert && (
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
        {tiltakDokumenter.length > 0 ? (
          <PagineringContainer>
            <PagineringsOversikt
              page={filter.page}
              pageSize={filter.pageSize}
              antall={tiltakDokumenter.length}
              maksAntall={pagination.totalCount}
              type="tiltaksdokumenter"
              onChangePageSize={(size) => {
                updateFilter({ page: 1, pageSize: size });
              }}
            />
            <Pagination
              size="small"
              page={filter.page}
              count={pagination.totalPages}
              onPageChange={(page) => updateFilter({ page })}
              data-version="v1"
            />
          </PagineringContainer>
        ) : null}
      </TabellWrapper>
    </>
  );
}
