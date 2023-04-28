import { Alert, Pagination, Table } from "@navikt/ds-react";
import { useState } from "react";
import { useAtom } from "jotai";
import { paginationAtom, tiltaksgjennomforingfilter } from "../../api/atoms";
import { SorteringTiltaksgjennomforinger } from "../../../../mulighetsrommet-api-client";
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
import { Sortering } from "./Types";

interface Props {
  skjulKolonner?: boolean;
}

export const TiltaksgjennomforingsTabell = ({
  skjulKolonner = false,
}: Props) => {
  const { data, isLoading, isError } = useAdminTiltaksgjennomforinger();
  const [page, setPage] = useAtom(paginationAtom);
  const [sort, setSort] = useState<Sortering>({
    orderBy: "navn",
    direction: "ascending",
  });
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilter);
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

  const handleSort = (sortKey: string) => {
    const direction =
      sort.direction === "ascending" ? "descending" : "ascending";

    setSort({
      orderBy: sortKey,
      direction,
    });

    setFilter({
      ...filter,
      sortering: `${sortKey}-${direction}` as SorteringTiltaksgjennomforinger,
    });
  };

  return (
    <div className={styles.tabell_wrapper}>
      <PagineringsOversikt
        page={page}
        antall={tiltaksgjennomforinger.length}
        maksAntall={pagination?.totalCount}
        type="tiltaksgjennomføringer"
      />

      <Table
        sort={sort!}
        onSortChange={(sortKey) => handleSort(sortKey!)}
        className={styles.tabell}
      >
        <Table.Header>
          <Table.Row className={styles.tiltaksgjennomforing_tabellrad}>
            <Table.ColumnHeader sortKey="navn" sortable>
              Tittel
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="tiltaksnummer" sortable>
              Tiltaksnr.
            </Table.ColumnHeader>
            {!skjulKolonner && (
              <>
                <Table.ColumnHeader>Arrangør</Table.ColumnHeader>
                <Table.ColumnHeader sortKey="tiltakstype" sortable>
                  Tiltakstype
                </Table.ColumnHeader>
              </>
            )}
            <Table.ColumnHeader sortKey="startdato" sortable>
              Startdato
            </Table.ColumnHeader>
            <Table.ColumnHeader sortKey="sluttdato" sortable>
              Sluttdato
            </Table.ColumnHeader>
            <Table.ColumnHeader>Status</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        {tiltaksgjennomforinger.length > 0 ? (
          <Table.Body>
            {tiltaksgjennomforinger.map((tiltaksgjennomforing, index) => {
              return (
                <Table.Row
                  key={index}
                  className={styles.tiltaksgjennomforing_tabellrad}
                >
                  <Table.DataCell
                    aria-label={`Navn på tiltaksgjennomforing: ${tiltaksgjennomforing.navn}`}
                    className={styles.title}
                  >
                    <Lenke
                      to={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}
                      data-testid="tiltaksgjennomforingrad"
                    >
                      {tiltaksgjennomforing.navn}
                    </Lenke>
                  </Table.DataCell>
                  <Table.DataCell
                    aria-label={`Tiltaksnummer: ${tiltaksgjennomforing.tiltaksnummer}`}
                  >
                    {tiltaksgjennomforing.tiltaksnummer}
                  </Table.DataCell>
                  {!skjulKolonner && (
                    <>
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
                    </>
                  )}
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
            })}
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
    </div>
  );
};
