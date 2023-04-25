import { Alert, Pagination, Table } from "@navikt/ds-react";
import { useState } from "react";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { useAtom } from "jotai";
import { avtalePaginationAtom } from "../../api/atoms";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import styles from "./Tabell.module.scss";
import { AVTALE_PAGE_SIZE } from "../../constants";
import { capitalizeEveryWord, formaterDato } from "../../utils/Utils";
import { Avtalestatus } from "../statuselementer/Avtalestatus";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";

export const AvtaleTabell = () => {
  const { data, isLoading, isError } = useAvtaler();
  const [page, setPage] = useAtom(avtalePaginationAtom);
  const [sort, setSort] = useState();
  const rowsPerPage = 15;
  const pagination = data?.pagination;
  const avtaler = data?.data ?? [];

  if (!avtaler && isLoading) {
    return <Laster size="xlarge" tekst="Laster avtaler..." />;
  }

  if (!avtaler) {
    return <Alert variant="info">Fant ingen avtaler</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">Vi hadde problemer med henting av avtaler</Alert>
    );
  }

  const avtalerForSide = avtaler
    .sort((a, b) => {
      const sortOrDefault = sort || {
        orderBy: "navn",
        direction: "ascending",
      };

      const comparator = (a: any, b: any, orderBy: string | number) => {
        const compare = (item1: any, item2: any) => {
          if (item2 < item1 || item2 === undefined) {
            return -1;
          }
          if (item2 > item1) {
            return 1;
          }
          return 0;
        };

        if (orderBy === "leverandor") {
          return compare(a.leverandor.navn, b.leverandor.navn);
        } else if (orderBy === "enhet") {
          return compare(a.navEnhet.navn, b.navEnhet.navn);
        } else if (orderBy === "startdato") {
          const dateB = new Date(b.startDato);
          const dateA = new Date(a.startDato);
          return compare(dateA, dateB);
        } else if (orderBy === "sluttdato") {
          const dateB = new Date(b.sluttDato);
          const dateA = new Date(a.sluttDato);
          return compare(dateA, dateB);
        } else if (orderBy === "status") {
          return compare(a.avtalestatus, b.avtalestatus);
        } else {
          return compare(a[orderBy], b[orderBy]);
        }
      };
      return sortOrDefault.direction === "ascending"
        ? comparator(b, a, sortOrDefault.orderBy)
        : comparator(a, b, sortOrDefault.orderBy);
    })
    .slice((page - 1) * rowsPerPage, page * rowsPerPage);

  const handleSort = (sortKey: string) => {
    setSort(
      // @ts-ignore
      sort && sortKey === sort.orderBy && sort.direction === "descending"
        ? undefined
        : {
            // @ts-ignore
            orderBy: sortKey,
            direction:
              // @ts-ignore
              sort && sortKey === sort.orderBy && sort.direction === "ascending"
                ? "descending"
                : "ascending",
          }
    );
  };
  return (
    <>
      <PagineringsOversikt
        page={page}
        antall={avtaler.length}
        maksAntall={pagination?.totalCount}
        type="avtaler"
      />
      <Table sort={sort!} onSortChange={(sortKey) => handleSort(sortKey!)}>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader sortKey="navn" sortable>
              Tittel
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="leverandor" sortable>
              Leverandør
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="enhet" sortable>
              Enhet
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="startdato" sortable>
              Startdato
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="sluttdato" sortable>
              Sluttdato
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="status" sortable>
              Status
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        {avtaler.length > 0 ? (
          <Table.Body className={styles.tabellbody}>
            {avtalerForSide.map((avtale, index) => {
              return (
                <Table.Row key={index}>
                  <Table.HeaderCell
                    scope="row"
                    aria-label={`Avtalenavn: ${avtale.navn}`}
                  >
                    <Lenke to={`/avtaler/${avtale.id}`} data-testid="avtalerad">
                      {avtale.navn}
                    </Lenke>
                  </Table.HeaderCell>
                  <Table.DataCell
                    aria-label={`Leverandør: ${avtale.leverandor?.navn}`}
                  >
                    {capitalizeEveryWord(avtale.leverandor?.navn, [
                      "og",
                      "i",
                    ]) || ""}
                  </Table.DataCell>
                  <Table.DataCell
                    aria-label={`NAV-enhet: ${
                      avtale.navEnhet?.navn || avtale.navEnhet?.enhetsnummer
                    }`}
                  >
                    {avtale.navEnhet?.navn || avtale?.navEnhet?.enhetsnummer}
                  </Table.DataCell>
                  <Table.DataCell
                    aria-label={`Startdato: ${formaterDato(avtale.startDato)}`}
                  >
                    {formaterDato(avtale.startDato)}
                  </Table.DataCell>
                  <Table.DataCell
                    aria-label={`Sluttdato: ${formaterDato(avtale.sluttDato)}`}
                  >
                    {formaterDato(avtale.sluttDato)}
                  </Table.DataCell>
                  <Table.DataCell>
                    <Avtalestatus avtale={avtale} />
                  </Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
        ) : (
          <></>
        )}
      </Table>
      {avtaler.length > 0 ? (
        <PagineringContainer>
          <PagineringsOversikt
            page={page}
            antall={avtaler.length}
            maksAntall={pagination?.totalCount}
            type="avtaler"
          />
          <Pagination
            className={styles.pagination}
            size="small"
            data-testid="paginering"
            page={page}
            onPageChange={setPage}
            count={Math.ceil(
              (pagination?.totalCount ?? AVTALE_PAGE_SIZE) / AVTALE_PAGE_SIZE
            )}
            data-version="v1"
          />
        </PagineringContainer>
      ) : null}
    </>
  );
};
