import { Alert, Button, Pagination, Table, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { WritableAtom, useAtom } from "jotai";
import { OpenAPI, SorteringAvtaler } from "mulighetsrommet-api-client";
import { createRef, useEffect, useState } from "react";
import { Lenke } from "../../../../frontend-common/components/lenke/Lenke";
import { AvtaleFilter } from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { APPLICATION_NAME } from "../../constants";
import { useSort } from "../../hooks/useSort";
import {
  capitalizeEveryWord,
  createQueryParamsForExcelDownload,
  formaterDato,
  formaterNavEnheter,
} from "../../utils/Utils";
import { ShowOpphavValue } from "../debug/ShowOpphavValue";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { AvtalestatusTag } from "../statuselementer/AvtalestatusTag";
import styles from "./Tabell.module.scss";

async function lastNedFil(filter: AvtaleFilter) {
  const headers = new Headers();
  headers.append("Nav-Consumer-Id", APPLICATION_NAME);

  if (import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN) {
    headers.append(
      "Authorization",
      `Bearer ${import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN}`,
    );
  }
  headers.append("accept", "application/json");

  const queryParams = createQueryParamsForExcelDownload(filter);

  return await fetch(`${OpenAPI.BASE}/api/v1/internal/avtaler/excel?${queryParams}`, {
    headers,
  });
}

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
}

export const AvtaleTabell = ({ filterAtom }: Props) => {
  const [sort, setSort] = useSort("navn");
  const [filter, setFilter] = useAtom(filterAtom);
  const [lasterExcel, setLasterExcel] = useState(false);
  const [excelUrl, setExcelUrl] = useState("");

  const { data, isLoading } = useAvtaler(filter);

  const link = createRef<HTMLAnchorElement>();

  async function lastNedExcel() {
    setLasterExcel(true);
    if (excelUrl) {
      setExcelUrl("");
    }

    const excelFil = await lastNedFil(filter);
    const blob = await excelFil.blob();
    const url = URL.createObjectURL(blob);
    setExcelUrl(url);
    setLasterExcel(false);
  }

  useEffect(() => {
    if (link.current && excelUrl) {
      link.current.download = "avtaler.xlsx";
      link.current.href = excelUrl;

      link.current.click();
      URL.revokeObjectURL(excelUrl);
    }
  }, [excelUrl]);

  function updateFilter(newFilter: Partial<AvtaleFilter>) {
    setFilter({ ...filter, ...newFilter });
  }

  const handleSort = (sortKey: string) => {
    // Hvis man bytter sortKey starter vi med ascending
    const direction =
      sort.orderBy === sortKey
        ? sort.direction === "descending"
          ? "ascending"
          : "descending"
        : "ascending";

    setSort({
      orderBy: sortKey,
      direction,
    });

    updateFilter({
      sortering: `${sortKey}-${direction}` as SorteringAvtaler,
      page: sort.orderBy !== sortKey || sort.direction !== direction ? 1 : filter.page,
    });
  };

  if (!data || isLoading) {
    return <Laster size="xlarge" tekst="Laster avtaler..." />;
  }

  const { pagination, data: avtaler } = data;

  return (
    <div className={classNames(styles.tabell_wrapper, styles.avtaletabell)}>
      <div className={styles.tabell_topp_container}>
        <div className={styles.flex}>
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
        </div>
        <div>
          <Button
            icon={<ExcelIkon />}
            variant="tertiary"
            onClick={lastNedExcel}
            disabled={lasterExcel}
            type="button"
          >
            {lasterExcel ? "Henter Excel-fil..." : "Eksporter tabellen til Excel"}
          </Button>
          <a style={{ display: "none" }} ref={link}></a>
        </div>
      </div>
      {avtaler.length === 0 ? (
        <Alert variant="info">Fant ingen avtaler</Alert>
      ) : (
        <Table
          sort={sort!}
          onSortChange={(sortKey) => handleSort(sortKey!)}
          className={styles.tabell}
        >
          <Table.Header>
            <Table.Row className={styles.avtale_tabellrad}>
              <Table.ColumnHeader sortKey="navn" sortable>
                Avtalenavn
              </Table.ColumnHeader>
              <Table.ColumnHeader>Avtalenummer</Table.ColumnHeader>
              <Table.ColumnHeader sortKey="leverandor" sortable>
                Leverandør
              </Table.ColumnHeader>
              <Table.ColumnHeader>Region</Table.ColumnHeader>
              <Table.ColumnHeader sortKey="startdato" sortable>
                Startdato
              </Table.ColumnHeader>
              <Table.ColumnHeader sortKey="sluttdato" sortable>
                Sluttdato
              </Table.ColumnHeader>
              <Table.ColumnHeader>Status</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {avtaler.map((avtale, index) => {
              return (
                <Table.Row key={index} className={styles.avtale_tabellrad}>
                  <Table.DataCell
                    aria-label={`Avtalenavn: ${avtale.navn}`}
                    className={styles.title}
                  >
                    <VStack>
                      <Lenke to={`/avtaler/${avtale.id}`} data-testid="avtaletabell_tittel">
                        {avtale.navn}
                      </Lenke>
                      <ShowOpphavValue value={avtale.opphav} />
                    </VStack>
                  </Table.DataCell>
                  <Table.DataCell aria-label={`Avtalenummer: ${avtale?.avtalenummer ?? "N/A"}`}>
                    {avtale?.avtalenummer}
                  </Table.DataCell>
                  <Table.DataCell aria-label={`Leverandør: ${avtale.leverandor?.navn}`}>
                    {capitalizeEveryWord(avtale.leverandor?.navn, ["og", "i"]) || ""}
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
                      undefined,
                      avtale.kontorstruktur?.map((struktur) => ({
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
                    <AvtalestatusTag avtale={avtale} />
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
            className={styles.pagination}
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
    </div>
  );
};

function ExcelIkon() {
  return (
    <svg width="24" height="24" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M5.25 4.5C5.25 3.80964 5.80964 3.25 6.5 3.25H14C14.1989 3.25 14.3897 3.32902 14.5303 3.46967L18.5303 7.46967C18.671 7.61032 18.75 7.80109 18.75 8V12.25H21C21.4142 12.25 21.75 12.5858 21.75 13V20C21.75 20.4142 21.4142 20.75 21 20.75H3C2.58579 20.75 2.25 20.4142 2.25 20V13C2.25 12.5858 2.58579 12.25 3 12.25H5.25V4.5ZM6.75 12.25H17.25V8.75H14.5C13.8096 8.75 13.25 8.19036 13.25 7.5V4.75H6.75V12.25ZM14.75 5.81066L16.1893 7.25H14.75V5.81066ZM16.4093 19.0701C16.05 19.0701 15.7373 19.0094 15.4713 18.8881C15.2053 18.7667 15 18.5964 14.8553 18.3771C14.7106 18.1577 14.6383 17.8987 14.6383 17.6001H15.6883C15.6883 17.7727 15.7536 17.9104 15.8843 18.0131C16.0196 18.1111 16.2016 18.1601 16.4303 18.1601C16.6496 18.1601 16.82 18.1111 16.9413 18.0131C17.0673 17.9151 17.1303 17.7797 17.1303 17.6071C17.1303 17.4577 17.0836 17.3294 16.9903 17.2221C16.897 17.1147 16.7663 17.0424 16.5983 17.0051L16.0803 16.8861C15.6463 16.7834 15.308 16.5944 15.0653 16.3191C14.8273 16.0391 14.7083 15.6984 14.7083 15.2971C14.7083 14.9984 14.776 14.7394 14.9113 14.5201C15.0513 14.2961 15.2473 14.1234 15.4993 14.0021C15.7513 13.8807 16.05 13.8201 16.3953 13.8201C16.918 13.8201 17.331 13.9507 17.6343 14.2121C17.9423 14.4687 18.0963 14.8164 18.0963 15.2551H17.0463C17.0463 15.0917 16.988 14.9634 16.8713 14.8701C16.7593 14.7767 16.596 14.7301 16.3813 14.7301C16.1806 14.7301 16.0266 14.7767 15.9193 14.8701C15.812 14.9587 15.7583 15.0871 15.7583 15.2551C15.7583 15.4044 15.8003 15.5327 15.8843 15.6401C15.973 15.7427 16.0966 15.8127 16.2553 15.8501L16.8013 15.9761C17.254 16.0787 17.597 16.2654 17.8303 16.5361C18.0636 16.8021 18.1803 17.1427 18.1803 17.5581C18.1803 17.8567 18.1056 18.1204 17.9563 18.3491C17.8116 18.5777 17.6063 18.7551 17.3403 18.8811C17.079 19.0071 16.7686 19.0701 16.4093 19.0701ZM7.00677 16.3611L5.63477 19.0001H6.76177L7.37777 17.7401C7.4291 17.6374 7.4711 17.5417 7.50377 17.4531C7.53643 17.3644 7.55977 17.2967 7.57377 17.2501C7.59243 17.2967 7.62043 17.3644 7.65777 17.4531C7.6951 17.5417 7.7371 17.6374 7.78377 17.7401L8.39977 19.0001H9.55477L8.18277 16.3611L9.46377 13.8901H8.33677L7.79077 15.0101C7.7441 15.1127 7.70443 15.2061 7.67177 15.2901C7.6391 15.3741 7.6181 15.4347 7.60877 15.4721C7.59943 15.4347 7.5761 15.3741 7.53877 15.2901C7.5061 15.2061 7.4641 15.1127 7.41277 15.0101L6.88077 13.8901H5.72577L7.00677 16.3611ZM10.567 13.8901V19.0001H13.682V18.0201H11.617V13.8901H10.567Z"
        fill="#0067C5"
      />
    </svg>
  );
}
