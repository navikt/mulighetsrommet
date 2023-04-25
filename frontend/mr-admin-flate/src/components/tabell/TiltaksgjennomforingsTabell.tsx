import { Alert, Pagination, Table } from "@navikt/ds-react";
import { useState } from "react";
import { useAtom } from "jotai";
import { paginationAtom } from "../../api/atoms";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import styles from "./Tabell.module.scss";
import { PAGE_SIZE } from "../../constants";
import { formaterDato } from "../../utils/Utils";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { Tiltaksgjennomforingstatus } from "../statuselementer/Tiltaksgjennomforingstatus";
import pageStyles from "../../pages/Page.module.scss";

export const TiltaksgjennomforingsTabell = () => {
  const { data, isLoading, isError } = useAdminTiltaksgjennomforinger();
  const [page, setPage] = useAtom(paginationAtom);
  const [sort, setSort] = useState();
  const rowsPerPage = 15;
  const pagination = data?.pagination;
  const tiltaksgjennomforinger = data?.data ?? [];

  if (!tiltaksgjennomforinger && isLoading) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  if (!tiltaksgjennomforinger) {
    return <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Vi hadde problemer med henting av tiltaksgjennomføringer
      </Alert>
    );
  }

  const tiltaksgjennomforingerForSide = tiltaksgjennomforinger
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
        } else if (orderBy === "tiltaksnummer") {
          return compare(a.tiltaksnummer, b.tiltaksnummer);
        } else if (orderBy === "arrangor") {
          return compare(a.virksomhetsnavn, b.virksomhetsnavn);
        } else if (orderBy === "tiltakstype") {
          return compare(a.tiltakstype.navn, b.tiltakstype.navn);
        } else if (orderBy === "startdato") {
          const dateB = new Date(b.startDato);
          const dateA = new Date(a.startDato);
          return compare(dateA, dateB);
        } else if (orderBy === "sluttdato") {
          const dateB = new Date(b.sluttDato);
          const dateA = new Date(a.sluttDato);
          return compare(dateA, dateB);
        } else if (orderBy === "status") {
          return compare(a.status, b.status);
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
        antall={tiltaksgjennomforinger.length}
        maksAntall={pagination?.totalCount}
        type="tiltaksgjennomføringer"
      />
      <Table sort={sort!} onSortChange={(sortKey) => handleSort(sortKey!)}>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader sortKey="navn" sortable>
              Tittel
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltaksnummer" sortable>
              Tiltaksnr.
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="arrangor" sortable>
              Arrangør
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltakstype" sortable>
              Tiltakstype
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
        {tiltaksgjennomforinger.length > 0 ? (
          <Table.Body className={styles.tabellbody}>
            {tiltaksgjennomforingerForSide.map(
              (tiltaksgjennomforing, index) => {
                return (
                  <Table.Row key={index}>
                    <Table.HeaderCell
                      scope="row"
                      aria-label={`Navn på tiltaksgjennomforing: ${tiltaksgjennomforing.navn}`}
                    >
                      <Lenke
                        to={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}
                        data-testid="tiltaksgjennomforingrad"
                      >
                        {tiltaksgjennomforing.navn}
                      </Lenke>
                    </Table.HeaderCell>

                    <Table.DataCell
                      aria-label={`Tiltaksnummer: ${tiltaksgjennomforing.tiltaksnummer}`}
                    >
                      {tiltaksgjennomforing.tiltaksnummer}
                    </Table.DataCell>

                    <Table.DataCell
                      aria-label={`Virksomhetsnavn: ${tiltaksgjennomforing.virksomhetsnavn}`}
                    >
                      {tiltaksgjennomforing.virksomhetsnavn}
                    </Table.DataCell>

                    <Table.DataCell
                      aria-label={`Tiltakstypenavn: ${tiltaksgjennomforing.tiltakstype.navn}`}
                    >
                      {tiltaksgjennomforing.tiltakstype.navn}
                    </Table.DataCell>
                    <Table.DataCell
                      title={`Startdato ${formaterDato(
                        tiltaksgjennomforing.startDato
                      )}`}
                      aria-label={`Startdato: ${formaterDato(
                        tiltaksgjennomforing.startDato
                      )}`}
                    >
                      {formaterDato(tiltaksgjennomforing.startDato)}
                    </Table.DataCell>
                    <Table.DataCell
                      title={`Sluttdato ${formaterDato(
                        tiltaksgjennomforing.sluttDato
                      )}, "-"`}
                      aria-label={
                        tiltaksgjennomforing.sluttDato
                          ? `Sluttdato: ${formaterDato(
                              tiltaksgjennomforing.sluttDato,
                              "-"
                            )}`
                          : undefined // Noen gjennomføringer har ikke sluttdato så da setter vi heller ikke aria-label for da klager reactA11y
                      }
                    >
                      {formaterDato(tiltaksgjennomforing.sluttDato)}
                    </Table.DataCell>
                    <Table.DataCell>
                      <Tiltaksgjennomforingstatus
                        tiltaksgjennomforing={tiltaksgjennomforing}
                      />
                    </Table.DataCell>
                  </Table.Row>
                );
              }
            )}
          </Table.Body>
        ) : (
          <></>
        )}
      </Table>
      {tiltaksgjennomforinger.length > 0 ? (
        <PagineringContainer>
          <PagineringsOversikt
            page={page}
            antall={tiltaksgjennomforinger.length}
            maksAntall={pagination?.totalCount}
            type="tiltaksgjennomføringer"
          />
          <Pagination
            className={pageStyles.pagination}
            size="small"
            data-testid="paginering"
            page={page}
            onPageChange={setPage}
            count={Math.ceil((pagination?.totalCount ?? PAGE_SIZE) / PAGE_SIZE)}
            data-version="v1"
          />
        </PagineringContainer>
      ) : null}
    </>
  );
};
