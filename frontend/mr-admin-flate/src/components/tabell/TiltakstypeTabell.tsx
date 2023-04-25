import { Alert, Pagination, Table } from "@navikt/ds-react";
import { useState } from "react";
import { useAtom } from "jotai";
import { paginationAtom, tiltakstypeFilter } from "../../api/atoms";
import { Laster } from "../laster/Laster";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import styles from "./Tabell.module.scss";
import { PAGE_SIZE } from "../../constants";
import { formaterDato } from "../../utils/Utils";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import { Tiltakstypestatus } from "../statuselementer/Tiltakstypestatus";
import pageStyles from "../../pages/Page.module.scss";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";

export const TiltakstypeTabell = () => {
  const [page, setPage] = useAtom(paginationAtom);
  const [filter] = useAtom(tiltakstypeFilter);
  const { data, isLoading, isError } = useTiltakstyper(filter, page);
  const [sort, setSort] = useState();
  const rowsPerPage = 15;
  const pagination = data?.pagination;
  const tiltakstyper = data?.data ?? [];

  if (!tiltakstyper && isLoading) {
    return <Laster size="xlarge" tekst="Laster tiltakstyper..." />;
  }

  if (!tiltakstyper) {
    return <Alert variant="info">Fant ingen tiltakstyper</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Vi hadde problemer med henting av tiltakstyper
      </Alert>
    );
  }

  const tiltakstyperForSide = tiltakstyper
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

        if (orderBy === "status") {
          return compare(a.status, b.status);
        } else if (orderBy === "startdato") {
          const dateB = new Date(b.fraDato);
          const dateA = new Date(a.fraDato);
          return compare(dateA, dateB);
        } else if (orderBy === "sluttdato") {
          const dateB = new Date(b.tilDato);
          const dateA = new Date(a.tilDato);
          return compare(dateA, dateB);
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
        antall={tiltakstyper.length}
        maksAntall={pagination?.totalCount}
        type="tiltakstyper"
      />
      <Table sort={sort!} onSortChange={(sortKey) => handleSort(sortKey!)}>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader sortKey="navn" sortable>
              Tittel
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="status" sortable>
              Status
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="startdato" sortable>
              Startdato
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="sluttdato" sortable>
              Sluttdato
            </Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        {tiltakstyper.length > 0 ? (
          <Table.Body className={styles.tabellbody}>
            {tiltakstyperForSide.map((tiltakstype, index) => {
              return (
                <Table.Row key={index}>
                  <Table.HeaderCell
                    scope="row"
                    aria-label={`Navn på tiltakstype: ${tiltakstype.navn}`}
                  >
                    <Lenke to={`/tiltakstyper/${tiltakstype.id}`}>
                      {tiltakstype.navn}
                    </Lenke>
                  </Table.HeaderCell>
                  <Table.DataCell>
                    <Tiltakstypestatus tiltakstype={tiltakstype} />
                  </Table.DataCell>

                  <Table.DataCell
                    aria-label={`Startdato: ${formaterDato(
                      tiltakstype.fraDato
                    )}`}
                  >
                    {formaterDato(tiltakstype.fraDato)}
                  </Table.DataCell>
                  <Table.DataCell
                    aria-label={`Sluttdato: ${formaterDato(
                      tiltakstype.tilDato
                    )}`}
                  >
                    {formaterDato(tiltakstype.tilDato)}
                  </Table.DataCell>
                </Table.Row>
              );
            })}
          </Table.Body>
        ) : (
          <></>
        )}
      </Table>
      {tiltakstyper.length > 0 ? (
        <PagineringContainer>
          <>
            <PagineringsOversikt
              page={page}
              antall={tiltakstyper.length}
              maksAntall={pagination?.totalCount}
              type="tiltakstyper"
            />
            <Pagination
              className={pageStyles.pagination}
              size="small"
              data-testid="paginering"
              page={page}
              onPageChange={setPage}
              count={Math.ceil(
                (pagination?.totalCount ?? PAGE_SIZE) / PAGE_SIZE
              )}
              data-version="v1"
            />
          </>
        </PagineringContainer>
      ) : null}
    </>
  );
};
