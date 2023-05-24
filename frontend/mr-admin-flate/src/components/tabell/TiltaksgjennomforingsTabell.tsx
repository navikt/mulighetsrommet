import { Alert, Button, Pagination, Table } from "@navikt/ds-react";
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
import { useSort } from "../../hooks/useSort";
import React from "react";
import { PlusIcon } from "@navikt/aksel-icons";

interface ColumnHeader {
  sortKey: Kolonne;
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  { sortKey: "navn", tittel: "Tittel", sortable: true, width: "3fr" },
  {
    sortKey: "tiltaksnummer",
    tittel: "Tiltaksnr.",
    sortable: true,
    width: "1fr",
  },
  { sortKey: "arrangor", tittel: "Arrangør", sortable: false, width: "3fr" },
  {
    sortKey: "tiltakstype",
    tittel: "Tiltakstype",
    sortable: true,
    width: "3fr",
  },
  { sortKey: "startdato", tittel: "Startdato", sortable: true, width: "1fr" },
  { sortKey: "sluttdato", tittel: "Sluttdato", sortable: true, width: "1fr" },
  { sortKey: "status", tittel: "Status", sortable: true, width: "1fr" },
  { sortKey: "leggTilKnapp", tittel: "", sortable: false, width: "1fr" },
];

type Kolonne =
  | "navn"
  | "tiltaksnummer"
  | "tiltakstype"
  | "arrangor"
  | "startdato"
  | "sluttdato"
  | "status"
  | "leggTilKnapp";

interface Props {
  skjulKolonner: Partial<Record<Kolonne, boolean>>;
  leggTilNyGjennomforingModal?: boolean;
}

const SkjulKolonne = ({
  children,
  skjul,
}: {
  children: React.ReactNode;
  skjul: boolean;
}) => {
  return skjul ? null : <>{children}</>;
};

export const TiltaksgjennomforingsTabell = ({
  skjulKolonner,
  leggTilNyGjennomforingModal,
}: Props) => {
  const { data, isLoading, isError } = useAdminTiltaksgjennomforinger();
  const [page, setPage] = useAtom(paginationAtom);
  const [sort, setSort] = useSort("navn");
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilter);
  const pagination = data?.pagination;
  const tiltaksgjennomforinger = data?.data ?? [];

  if (
    (!tiltaksgjennomforinger || tiltaksgjennomforinger.length === 0) &&
    isLoading
  ) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  if (tiltaksgjennomforinger.length === 0) {
    return <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Vi hadde problemer med henting av tiltaksgjennomføringer
      </Alert>
    );
  }

  if (tiltaksgjennomforinger.length === 0) {
    return (
      <Alert variant="info">
        Det finnes ingen tiltaksgjennomføringer for avtalen.
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
      {!leggTilNyGjennomforingModal && (
        <PagineringsOversikt
          page={page}
          antall={tiltaksgjennomforinger.length}
          maksAntall={pagination?.totalCount}
          type="tiltaksgjennomføringer"
        />
      )}

      <Table
        sort={sort!}
        onSortChange={(sortKey) => handleSort(sortKey!)}
        className={styles.tabell}
      >
        <Table.Header>
          <Table.Row className={styles.tiltaksgjennomforing_tabellrad}>
            {headers
              .filter((header) => {
                return skjulKolonner ? !skjulKolonner[header.sortKey] : true;
              })
              .map((header) => (
                <Table.ColumnHeader
                  key={header.sortKey}
                  sortKey={header.sortKey}
                  sortable={
                    leggTilNyGjennomforingModal ? false : header.sortable
                  }
                  style={{ width: header.width }}
                >
                  {header.tittel}
                </Table.ColumnHeader>
              ))}
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
                  <SkjulKolonne skjul={!!skjulKolonner?.navn}>
                    <Table.DataCell
                      aria-label={`Navn på tiltaksgjennomforing: ${tiltaksgjennomforing.navn}`}
                      className={
                        leggTilNyGjennomforingModal ? "" : styles.title
                      }
                    >
                      {leggTilNyGjennomforingModal ? (
                        tiltaksgjennomforing.navn
                      ) : (
                        <Lenke
                          to={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}
                          data-testid="tiltaksgjennomforingrad"
                        >
                          {tiltaksgjennomforing.navn}
                        </Lenke>
                      )}
                    </Table.DataCell>
                  </SkjulKolonne>

                  <SkjulKolonne skjul={!!skjulKolonner?.tiltaksnummer}>
                    <Table.DataCell
                      aria-label={`Tiltaksnummer: ${tiltaksgjennomforing.tiltaksnummer}`}
                    >
                      {tiltaksgjennomforing.tiltaksnummer}
                    </Table.DataCell>
                  </SkjulKolonne>

                  <SkjulKolonne skjul={!!skjulKolonner?.arrangor}>
                    <Table.DataCell
                      aria-label={`Virksomhetsnavn: ${tiltaksgjennomforing.virksomhetsnavn}`}
                    >
                      {tiltaksgjennomforing.virksomhetsnavn}
                    </Table.DataCell>
                  </SkjulKolonne>

                  <SkjulKolonne skjul={!!skjulKolonner?.tiltakstype}>
                    <Table.DataCell
                      aria-label={`Tiltakstypenavn: ${tiltaksgjennomforing.tiltakstype.navn}`}
                    >
                      {tiltaksgjennomforing.tiltakstype.navn}
                    </Table.DataCell>
                  </SkjulKolonne>

                  <SkjulKolonne skjul={!!skjulKolonner?.startdato}>
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
                  </SkjulKolonne>

                  <SkjulKolonne skjul={!!skjulKolonner?.sluttdato}>
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
                  </SkjulKolonne>

                  <SkjulKolonne skjul={!!skjulKolonner?.status}>
                    <Table.DataCell>
                      <Tiltaksgjennomforingstatus
                        tiltaksgjennomforing={tiltaksgjennomforing}
                      />
                    </Table.DataCell>
                  </SkjulKolonne>
                  <SkjulKolonne skjul={!!skjulKolonner?.leggTilKnapp}>
                    <Table.DataCell>
                      <Button
                        variant="tertiary"
                        className={styles.legg_til_knapp}
                      >
                        <PlusIcon fontSize={22} />
                        Legg til
                      </Button>
                    </Table.DataCell>
                  </SkjulKolonne>
                </Table.Row>
              );
            })}
          </Table.Body>
        ) : null}
      </Table>
      {tiltaksgjennomforinger.length > 0 && !leggTilNyGjennomforingModal ? (
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
