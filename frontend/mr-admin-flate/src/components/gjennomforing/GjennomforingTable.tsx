import { useGjennomforinger } from "@/api/gjennomforing/useGjennomforinger";
import { EksporterTabellKnapp } from "@/components/eksporterTabell/EksporterTabellKnapp";
import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import { formaterNavEnheter } from "@/utils/Utils";
import { SorteringGjennomforinger } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";
import { Alert, BodyShort, Pagination, Table, Tag, VStack } from "@navikt/ds-react";
import React from "react";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";
import { useDownloadGjennomforingerAsExcel } from "@/api/gjennomforing/useDownloadGjennomforingerAsExcel";
import { GjennomforingStatusTag } from "@/components/statuselementer/GjennomforingStatusTag";
import { formaterDato } from "@mr/frontend-common/utils/date";

const SkjulKolonne = ({ children, skjul }: { children: React.ReactNode; skjul: boolean }) => {
  return skjul ? null : <>{children}</>;
};

interface Props {
  skjulKolonner?: Partial<Record<Kolonne, boolean>>;
  filter: GjennomforingFilterType;
  updateFilter: (values: Partial<GjennomforingFilterType>) => void;
  tagsHeight: number;
  filterOpen: boolean;
}

export function GjennomforingTable({
  skjulKolonner,
  filter,
  updateFilter,
  tagsHeight,
  filterOpen,
}: Props) {
  const sort = filter.sortering.tableSort;
  const {
    data: { pagination, data: gjennomforinger },
  } = useGjennomforinger(filter);

  const [lasterExcel, lastNedExcel] = useDownloadGjennomforingerAsExcel(filter);

  const handleSort = (sortKey: string) => {
    // Hvis man bytter sortKey starter vi med ascending
    const direction =
      sort.orderBy === sortKey
        ? sort.direction === "descending"
          ? "ascending"
          : "descending"
        : "ascending";

    updateFilter({
      sortering: {
        sortString: `${sortKey}-${direction}` as SorteringGjennomforinger,
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
            antall={gjennomforinger.length}
            maksAntall={pagination.totalCount}
            type="tiltaksgjennomføringer"
            onChangePageSize={(size) => {
              updateFilter({
                page: 1,
                pageSize: size,
              });
            }}
          />
          <EksporterTabellKnapp lastNedExcel={lastNedExcel} lasterExcel={lasterExcel} />
        </ToolbarMeny>
      </ToolbarContainer>
      <TabellWrapper>
        {gjennomforinger.length === 0 ? (
          <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>
        ) : (
          <Table
            sort={sort}
            onSortChange={(sortKey) => handleSort(sortKey)}
            data-testid="gjennomforing-tabell"
          >
            <Table.Header
              style={{
                top: `calc(${tagsHeight}px + 6.9rem)`,
              }}
            >
              <Table.Row>
                {headers
                  .filter((header) => {
                    return skjulKolonner ? !skjulKolonner[header.sortKey] : true;
                  })
                  .map((header) => (
                    <Table.ColumnHeader
                      key={header.sortKey}
                      sortKey={header.sortKey}
                      sortable={header.sortable}
                      style={{
                        width: header.width,
                      }}
                    >
                      {header.tittel}
                    </Table.ColumnHeader>
                  ))}
              </Table.Row>
            </Table.Header>
            {gjennomforinger.length > 0 ? (
              <Table.Body>
                {gjennomforinger.map((gjennomforing, index) => {
                  const formattertSluttDato = gjennomforing.sluttDato
                    ? formaterDato(gjennomforing.sluttDato)
                    : "-";
                  const formattertStartDato = formaterDato(gjennomforing.startDato);

                  return (
                    <Table.Row key={index}>
                      <SkjulKolonne skjul={!!skjulKolonner?.navn}>
                        <Table.DataCell aria-label={`Navn på gjennomforing: ${gjennomforing.navn}`}>
                          <VStack>
                            <Lenke
                              to={`/gjennomforinger/${gjennomforing.id}`}
                              data-testid="gjennomforing-tabell_tittel"
                            >
                              {gjennomforing.navn}
                            </Lenke>
                          </VStack>
                        </Table.DataCell>
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.enhet}>
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
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.tiltaksnummer}>
                        <Table.DataCell
                          aria-label={`Tiltaksnummer: ${gjennomforing.tiltaksnummer}`}
                        >
                          {gjennomforing.tiltaksnummer ?? "-"}
                        </Table.DataCell>
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.arrangor}>
                        <Table.DataCell
                          aria-label={`Virksomhetsnavn: ${gjennomforing.arrangor.navn}`}
                        >
                          <BodyShort size="small">{gjennomforing.arrangor.navn}</BodyShort>
                        </Table.DataCell>
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.startdato}>
                        <Table.DataCell
                          title={`Startdato ${formattertStartDato}`}
                          aria-label={`Startdato: ${formattertStartDato}`}
                        >
                          {formattertStartDato}
                        </Table.DataCell>
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.sluttdato}>
                        <Table.DataCell
                          title={`Sluttdato ${formattertSluttDato}`}
                          aria-label={
                            gjennomforing.sluttDato ? `Sluttdato ${formattertSluttDato}` : undefined // Noen gjennomføringer har ikke sluttdato så da setter vi heller ikke aria-label for da klager reactA11y
                          }
                        >
                          {formattertSluttDato}
                        </Table.DataCell>
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.status}>
                        <Table.DataCell>
                          <GjennomforingStatusTag status={gjennomforing.status} />
                        </Table.DataCell>
                      </SkjulKolonne>
                      <Table.DataCell>
                        <VStack align={"center"}>
                          {gjennomforing.publisert ? (
                            <Tag
                              aria-label="Tiltaket er publisert for alle"
                              title="Tiltaket er publisert for alle"
                              variant="success-filled"
                            >
                              Ja
                            </Tag>
                          ) : (
                            <span
                              // Denne span'en må være her så brukere av skjermlesere får beskjed om at tiltaket ikke er tilgjengelig.
                              // Klassen under gjør at elementet er usynlig for brukere som kan se, men skjermlesere kan fortsatt få tak i elementet
                              className="navds-sr-only"
                              title="Tiltaket er ikke publisert for alle"
                            />
                          )}
                        </VStack>
                      </Table.DataCell>
                    </Table.Row>
                  );
                })}
              </Table.Body>
            ) : null}
          </Table>
        )}
        {gjennomforinger.length > 0 ? (
          <PagineringContainer>
            <PagineringsOversikt
              page={filter.page}
              pageSize={filter.pageSize}
              antall={gjennomforinger.length}
              maksAntall={pagination.totalCount}
              type="tiltaksgjennomføringer"
            />
            <Pagination
              size="small"
              page={filter.page}
              count={pagination.totalPages}
              onPageChange={(page) => {
                updateFilter({ page });
              }}
              data-version="v1"
            />
          </PagineringContainer>
        ) : null}
      </TabellWrapper>
    </>
  );
}

interface ColumnHeader {
  sortKey: Kolonne;
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  {
    sortKey: "navn",
    tittel: "Tiltaksnavn",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "enhet",
    tittel: "Enhet",
    sortable: false,
    width: "2fr",
  },
  {
    sortKey: "tiltaksnummer",
    tittel: "Tiltaksnr.",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "arrangor",
    tittel: "Arrangør",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "startdato",
    tittel: "Startdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "sluttdato",
    tittel: "Sluttdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "status",
    tittel: "Status",
    sortable: false,
    width: "1fr",
  },
  {
    sortKey: "publisert",
    tittel: "Publisert",
    sortable: true,
    width: "1fr",
  },
];

type Kolonne =
  | "dupliser"
  | "navn"
  | "enhet"
  | "tiltaksnummer"
  | "tiltakstype"
  | "arrangor"
  | "startdato"
  | "sluttdato"
  | "status"
  | "publisert";
