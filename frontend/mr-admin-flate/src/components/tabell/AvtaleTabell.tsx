import { Alert, Pagination, Table } from "@navikt/ds-react";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { useAtom } from "jotai";
import { avtaleFilter, avtalePaginationAtom } from "../../api/atoms";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import styles from "./Tabell.module.scss";
import { AVTALE_PAGE_SIZE } from "../../constants";
import { capitalizeEveryWord, formaterDato } from "../../utils/Utils";
import { Avtalestatus } from "../statuselementer/Avtalestatus";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import { SorteringAvtaler } from "mulighetsrommet-api-client";
import { useState } from "react";
import { Sortering } from "./Utils";

export const AvtaleTabell = () => {
  const { data, isLoading, isError } = useAvtaler();
  const [filter, setFilter] = useAtom(avtaleFilter);
  const [page, setPage] = useAtom(avtalePaginationAtom);
  const [sort, setSort] = useState<Sortering>({
    orderBy: "navn",
    direction: "ascending",
  });
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

  const handleSort = (sortKey: string) => {
    const direction =
      sort.direction === "ascending" ? "descending" : "ascending";

    setSort({
      orderBy: sortKey,
      direction,
    });

    setFilter({
      ...filter,
      sortering: `${sortKey}-${direction}` as SorteringAvtaler,
    });
  };

  return (
    <>
      <PagineringsOversikt
        page={page}
        antall={avtaler.length}
        maksAntall={pagination?.totalCount}
        type="avtaler"
      />
      <Table
        sort={sort!}
        onSortChange={(sortKey) => handleSort(sortKey!)}
        className={styles.table}
      >
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
            <Table.ColumnHeader>Status</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        {avtaler.length > 0 ? (
          <Table.Body>
            {avtaler.map((avtale, index) => {
              return (
                <Table.Row key={index}>
                  <Table.DataCell
                    scope="row"
                    aria-label={`Avtalenavn: ${avtale.navn}`}
                    className={styles.title}
                  >
                    <Lenke to={`/avtaler/${avtale.id}`} data-testid="avtalerad">
                      {avtale.navn}
                    </Lenke>
                  </Table.DataCell>
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
