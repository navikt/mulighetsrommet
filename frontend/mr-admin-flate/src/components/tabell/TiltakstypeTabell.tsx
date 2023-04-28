import { Alert, Pagination, Table } from "@navikt/ds-react";
import { useState } from "react";
import { useAtom } from "jotai";
import { paginationAtom, tiltakstypeFilter } from "../../api/atoms";
import { SorteringTiltakstyper } from "../../../../mulighetsrommet-api-client";
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
import { Sortering } from "./Types";

export const TiltakstypeTabell = () => {
  const [page, setPage] = useAtom(paginationAtom);
  const [filter, setFilter] = useAtom(tiltakstypeFilter);
  const { data, isLoading, isError } = useTiltakstyper(filter, page);
  const [sort, setSort] = useState<Sortering>({
    orderBy: "navn",
    direction: "ascending",
  });
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

  const handleSort = (sortKey: string) => {
    const direction =
      sort.direction === "ascending" ? "descending" : "ascending";

    setSort({
      orderBy: sortKey,
      direction,
    });

    setFilter({
      ...filter,
      sortering: `${sortKey}-${direction}` as SorteringTiltakstyper,
    });
  };
  return (
    <div className={styles.tabell_wrapper}>
      <PagineringsOversikt
        page={page}
        antall={tiltakstyper.length}
        maksAntall={pagination?.totalCount}
        type="tiltakstyper"
      />
      <Table
        sort={sort!}
        onSortChange={(sortKey) => handleSort(sortKey!)}
        className={styles.tabell}
      >
        <Table.Header>
          <Table.Row className={styles.tiltakstype_tabellrad}>
            <Table.ColumnHeader sortKey="navn" sortable>
              Tittel
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
        {tiltakstyper.length > 0 ? (
          <Table.Body>
            {tiltakstyper.map((tiltakstype, index) => {
              return (
                <Table.Row key={index} className={styles.tiltakstype_tabellrad}>
                  <Table.DataCell
                    aria-label={`Navn på tiltakstype: ${tiltakstype.navn}`}
                    className={styles.title}
                  >
                    <Lenke
                      to={`/tiltakstyper/${tiltakstype.id}`}
                      data-testid="tiltakstyperad"
                    >
                      {tiltakstype.navn}
                    </Lenke>
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
                  <Table.DataCell>
                    <Tiltakstypestatus tiltakstype={tiltakstype} />
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
            count={Math.ceil((pagination?.totalCount ?? PAGE_SIZE) / PAGE_SIZE)}
            data-version="v1"
          />
        </PagineringContainer>
      ) : null}
    </div>
  );
};
