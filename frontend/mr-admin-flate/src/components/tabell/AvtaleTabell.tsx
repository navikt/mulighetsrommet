import { Alert, Pagination, Table } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { SorteringAvtaler } from "mulighetsrommet-api-client";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import { avtaleFilter, avtalePaginationAtom } from "../../api/atoms";
import { useAvtaler } from "../../api/avtaler/useAvtaler";
import { AVTALE_PAGE_SIZE } from "../../constants";
import { useSort } from "../../hooks/useSort";
import { capitalizeEveryWord, formaterDato } from "../../utils/Utils";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { Avtalestatus } from "../statuselementer/Avtalestatus";
import styles from "./Tabell.module.scss";

export const AvtaleTabell = () => {
  const { data, isLoading, isError } = useAvtaler();
  const [filter, setFilter] = useAtom(avtaleFilter);
  const [page, setPage] = useAtom(avtalePaginationAtom);
  const [sort, setSort] = useSort("navn");
  const pagination = data?.pagination;
  const avtaler = data?.data ?? [];

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

  if ((!avtaler || avtaler.length === 0) && isLoading) {
    return <Laster size="xlarge" tekst="Laster avtaler..." />;
  }

  if (isError) {
    return (
      <Alert variant="error">Vi hadde problemer med henting av avtaler</Alert>
    );
  }

  if (avtaler.length === 0) {
    return <Alert variant="info">Fant ingen avtaler</Alert>;
  }

  return (
    <div className={styles.tabell_wrapper}>
      <PagineringsOversikt
        page={page}
        antall={avtaler.length}
        maksAntall={pagination?.totalCount}
        type="avtaler"
      />
      <Table
        sort={sort!}
        onSortChange={(sortKey) => handleSort(sortKey!)}
        className={styles.tabell}
      >
        <Table.Header>
          <Table.Row className={styles.avtale_tabellrad}>
            <Table.ColumnHeader sortKey="navn" sortable>
              Tittel
            </Table.ColumnHeader>
            <Table.ColumnHeader>Leverandør</Table.ColumnHeader>
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
        <Table.Body>
          {avtaler.map((avtale, index) => {
            return (
              <Table.Row key={index} className={styles.avtale_tabellrad}>
                <Table.DataCell
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
                  {capitalizeEveryWord(avtale.leverandor?.navn, ["og", "i"]) ||
                    ""}
                </Table.DataCell>
                <Table.DataCell
                  aria-label={`NAV-enhet: ${
                    avtale.navRegion?.navn || avtale.navRegion?.enhetsnummer
                  }`}
                >
                  {avtale.navRegion?.navn || avtale.navRegion?.enhetsnummer}
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
    </div>
  );
};
