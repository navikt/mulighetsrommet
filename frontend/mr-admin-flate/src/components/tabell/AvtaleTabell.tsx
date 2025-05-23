import { AvtaleFilterType } from "@/api/atoms";
import { EksporterTabellKnapp } from "@/components/eksporterTabell/EksporterTabellKnapp";
import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import {
  capitalizeEveryWord,
  createQueryParamsForExcelDownloadForAvtale,
  formaterDato,
  formaterNavEnheter,
} from "@/utils/Utils";
import { AvtalerService, SorteringAvtaler } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";
import { Alert, Pagination, Table, VStack } from "@navikt/ds-react";
import { createRef, useEffect, useState } from "react";
import { useAvtaler } from "@/api/avtaler/useAvtaler";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { AvtaleStatusTag } from "../statuselementer/AvtaleStatusTag";

interface Props {
  filter: AvtaleFilterType;
  updateFilter: (values: Partial<AvtaleFilterType>) => void;
  tagsHeight: number;
  filterOpen: boolean;
}

export function AvtaleTabell({ filter, updateFilter, tagsHeight, filterOpen }: Props) {
  const [lasterExcel, setLasterExcel] = useState(false);
  const [excelUrl, setExcelUrl] = useState("");
  const sort = filter.sortering.tableSort;
  const { data, isLoading } = useAvtaler(filter);

  const link = createRef<HTMLAnchorElement>();

  async function lastNedFil(filter: AvtaleFilterType) {
    const query = createQueryParamsForExcelDownloadForAvtale(filter);
    const { data } = await AvtalerService.lastNedAvtalerSomExcel(query);
    return data;
  }

  async function lastNedExcel() {
    setLasterExcel(true);
    if (excelUrl) {
      setExcelUrl("");
    }

    const excelFil = await lastNedFil(filter);
    const url = URL.createObjectURL(excelFil);
    setExcelUrl(url);
    setLasterExcel(false);
  }

  useEffect(() => {
    if (link.current && excelUrl) {
      link.current.download = "avtaler.xlsx";
      link.current.href = excelUrl;

      link.current.click();
      URL.revokeObjectURL(excelUrl);
      link.current = null;
      setExcelUrl("");
    }
  }, [excelUrl, link]);

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
        sortString: `${sortKey}-${direction}` as SorteringAvtaler,
        tableSort: { orderBy: sortKey, direction },
      },
      page: sort.orderBy !== sortKey || sort.direction !== direction ? 1 : filter.page,
    });
  };

  if (!data || isLoading) {
    return <Laster size="xlarge" tekst="Laster avtaler..." />;
  }

  const { pagination, data: avtaler } = data;

  return (
    <>
      <ToolbarContainer tagsHeight={tagsHeight} filterOpen={filterOpen}>
        <ToolbarMeny>
          <PagineringsOversikt
            page={filter.page}
            pageSize={filter.pageSize}
            antall={avtaler.length}
            maksAntall={pagination.totalCount}
            type="avtaler"
            onChangePageSize={(value) => {
              updateFilter({
                page: 1,
                pageSize: value,
              });
            }}
          />
          <EksporterTabellKnapp lastNedExcel={lastNedExcel} lasterExcel={lasterExcel} />
          <a style={{ display: "none" }} ref={link}></a>
        </ToolbarMeny>
      </ToolbarContainer>
      <TabellWrapper>
        {avtaler.length === 0 ? (
          <Alert variant="info">Fant ingen avtaler</Alert>
        ) : (
          <Table
            sort={sort!}
            onSortChange={(sortKey) => handleSort(sortKey!)}
            className="bg-white border-separate border-spacing-0 border-t border-gray-200"
          >
            <Table.Header
              style={{
                top: `calc(${tagsHeight}px + 7.8rem)`,
              }}
              className="sticky bg-white"
            >
              <Table.Row className="hover:bg-gray-50">
                {headers.map((header) => (
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
            <Table.Body>
              {avtaler.map((avtale, index) => {
                return (
                  <Table.Row key={index} className="hover:bg-gray-50">
                    <Table.DataCell aria-label={`Avtalenavn: ${avtale.navn}`} className="underline">
                      <VStack>
                        <Lenke to={`/avtaler/${avtale.id}`} data-testid="avtaletabell_tittel">
                          {avtale.navn}
                        </Lenke>
                      </VStack>
                    </Table.DataCell>
                    <Table.DataCell aria-label={`Avtalenummer: ${avtale?.avtalenummer ?? "N/A"}`}>
                      {avtale?.avtalenummer}
                    </Table.DataCell>
                    <Table.DataCell aria-label={`Tiltaksarrangør: ${avtale.arrangor?.navn}`}>
                      {capitalizeEveryWord(avtale.arrangor?.navn, ["og", "i"]) || ""}
                    </Table.DataCell>
                    <Table.DataCell
                      aria-label={`Regioner: ${avtale?.kontorstruktur
                        ?.map((struktur) => struktur.region.navn)
                        .join(", ")}`}
                      title={`Regioner: ${avtale?.kontorstruktur
                        ?.map((struktur) => struktur.region.navn)
                        .join(", ")}`}
                    >
                      {formaterNavEnheter(
                        avtale.kontorstruktur.map((struktur) => ({
                          navn: struktur.region.navn,
                          enhetsnummer: struktur.region.enhetsnummer,
                        })),
                      )}
                    </Table.DataCell>
                    <Table.DataCell aria-label={`Startdato: ${formaterDato(avtale.startDato)}`}>
                      {formaterDato(avtale.startDato)}
                    </Table.DataCell>
                    <Table.DataCell
                      aria-label={`Sluttdato: ${avtale.sluttDato ? formaterDato(avtale.sluttDato) : "-"}`}
                    >
                      {avtale.sluttDato ? formaterDato(avtale.sluttDato) : "-"}
                    </Table.DataCell>
                    <Table.DataCell>
                      <AvtaleStatusTag avtale={avtale} />
                    </Table.DataCell>
                  </Table.Row>
                );
              })}
            </Table.Body>
          </Table>
        )}
        {avtaler.length > 0 ? (
          <PagineringContainer>
            <PagineringsOversikt
              page={filter.page}
              pageSize={filter.pageSize}
              antall={avtaler.length}
              maksAntall={pagination.totalCount}
              type="avtaler"
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
    tittel: "Avtalenavn",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "avtalenummer",
    tittel: "Avtalenummer",
    sortable: false,
    width: "1fr",
  },
  {
    sortKey: "arrangor",
    tittel: "Arrangør",
    sortable: true,
    width: "2fr",
  },
  {
    sortKey: "region",
    tittel: "Region",
    sortable: false,
    width: "2fr",
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
];

type Kolonne =
  | "navn"
  | "avtalenummer"
  | "arrangor"
  | "region"
  | "startdato"
  | "sluttdato"
  | "status";
