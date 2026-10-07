import { Alert, Link, Pagination, Table, Tag, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { useTiltakBeskrivelser } from "@/api/tiltak-beskrivelse/useTiltakBeskrivelser";
import { TiltakBeskrivelseFilterType } from "@/pages/tiltak-beskrivelse/filter";
import { TiltakBeskrivelseKompaktDto } from "@tiltaksadministrasjon/api-client";
import { formaterNavEnheter } from "@/utils/Utils";
import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { PagineringsOversikt } from "@/components/paginering/PagineringOversikt";
import { PagineringContainer } from "@/components/paginering/PagineringContainer";
import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";

interface Props {
  filter: TiltakBeskrivelseFilterType;
  updateFilter: (values: Partial<TiltakBeskrivelseFilterType>) => void;
  tagsHeight: number;
  filterOpen: boolean;
}

export function TiltakBeskrivelseTable({ filter, updateFilter, tagsHeight, filterOpen }: Props) {
  const sort = filter.sortering.tableSort;
  const {
    data: { pagination, data: tiltakBeskrivelser },
  } = useTiltakBeskrivelser(filter);

  const handleSort = (sortKey: string) => {
    const direction =
      sort.orderBy === sortKey
        ? sort.direction === "descending"
          ? "ascending"
          : "descending"
        : "ascending";

    updateFilter({
      sortering: {
        sortString: `${sortKey}-${direction}`,
        tableSort: { orderBy: sortKey, direction },
      },
      page: sort.orderBy !== sortKey || sort.direction !== direction ? 1 : filter.page,
    });
  };

  return (
    <>
      <ToolbarContainer tagsHeight={tagsHeight} filterOpen={filterOpen}>
        <ToolbarMeny>
          <PagineringsOversikt
            page={filter.page}
            pageSize={filter.pageSize}
            antall={tiltakBeskrivelser.length}
            maksAntall={pagination.totalCount}
            type="tiltaksbeskrivelser"
            onChangePageSize={(size) => {
              updateFilter({ page: 1, pageSize: size });
            }}
          />
        </ToolbarMeny>
      </ToolbarContainer>
      <TabellWrapper>
        {tiltakBeskrivelser.length === 0 ? (
          <Alert variant="info">Fant ingen tiltaksbeskrivelser</Alert>
        ) : (
          <Table sort={sort} onSortChange={(sortKey) => handleSort(sortKey)}>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader sortKey="navn" sortable>
                  Navn
                </Table.ColumnHeader>
                <Table.ColumnHeader sortKey="tiltakstype" sortable>
                  Tiltakstype
                </Table.ColumnHeader>
                <Table.ColumnHeader>Enhet</Table.ColumnHeader>
                <Table.ColumnHeader sortKey="arrangor" sortable>
                  Arrangør
                </Table.ColumnHeader>
                <Table.ColumnHeader />
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {tiltakBeskrivelser.map((dokument: TiltakBeskrivelseKompaktDto) => (
                <Table.Row key={dokument.id}>
                  <Table.DataCell>
                    <Link as={ReactRouterLink} to={`/tiltak-beskrivelser/${dokument.id}`}>
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
                          title="Tiltaksbeskrivelsen er publisert og synlig for veileder i Modia"
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
        {tiltakBeskrivelser.length > 0 ? (
          <PagineringContainer>
            <PagineringsOversikt
              page={filter.page}
              pageSize={filter.pageSize}
              antall={tiltakBeskrivelser.length}
              maksAntall={pagination.totalCount}
              type="tiltaksbeskrivelser"
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
