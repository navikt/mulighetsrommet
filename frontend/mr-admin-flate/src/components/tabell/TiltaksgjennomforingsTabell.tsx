import { Alert, Checkbox, Pagination, Table } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { SorteringTiltaksgjennomforinger } from "mulighetsrommet-api-client";
import Lenke from "mulighetsrommet-veileder-flate/src/components/lenke/Lenke";
import React from "react";
import { paginationAtom, tiltaksgjennomforingfilter } from "../../api/atoms";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { useSort } from "../../hooks/useSort";
import pageStyles from "../../pages/Page.module.scss";
import { formaterDato, resetPaginering } from "../../utils/Utils";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { Tiltaksgjennomforingstatus } from "../statuselementer/Tiltaksgjennomforingstatus";
import styles from "./Tabell.module.scss";

interface ColumnHeader {
  sortKey: Kolonne;
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  { sortKey: "navn", tittel: "Tittel", sortable: true, width: "3fr" },
  { sortKey: "enhet", tittel: "Enhet", sortable: false, width: "2fr" },
  {
    sortKey: "tiltaksnummer",
    tittel: "Tiltaksnr.",
    sortable: true,
    width: "1fr",
  },
  { sortKey: "arrangor", tittel: "Arrangør", sortable: true, width: "3fr" },
  {
    sortKey: "tiltakstype",
    tittel: "Tiltakstype",
    sortable: true,
    width: "3fr",
  },
  { sortKey: "startdato", tittel: "Startdato", sortable: true, width: "1fr" },
  { sortKey: "sluttdato", tittel: "Sluttdato", sortable: true, width: "1fr" },
  { sortKey: "status", tittel: "Status", sortable: false, width: "1fr" },
];

type Kolonne =
  | "navn"
  | "enhet"
  | "tiltaksnummer"
  | "tiltakstype"
  | "arrangor"
  | "startdato"
  | "sluttdato"
  | "status";

interface Props {
  skjulKolonner?: Partial<Record<Kolonne, boolean>>;
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

export const TiltaksgjennomforingsTabell = ({ skjulKolonner }: Props) => {
  const { data, isLoading } = useAdminTiltaksgjennomforinger();
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

    setFilter({
      ...filter,
      sortering: `${sortKey}-${direction}` as SorteringTiltaksgjennomforinger,
    });
  };

  const formaterNavEnheter = (
    navRegion: string = "",
    navEnheter?: { navn?: string | null; enhetsnummer?: string }[],
  ): string => {
    const liste = [...(navEnheter || [])];
    if (!liste) return "";

    const forsteEnhet = liste.shift();
    if (!forsteEnhet) return navRegion;

    return `${forsteEnhet?.navn} ${
      liste.length > 0 ? `+ ${liste.length}` : ""
    }`;
  };

  return (
    <div className={styles.tabell_wrapper}>
      <div>
        <PagineringsOversikt
          page={page}
          antall={tiltaksgjennomforinger.length}
          maksAntall={pagination?.totalCount}
          type="tiltaksgjennomføringer"
          antallVises={filter.antallGjennomforingerVises}
          setAntallVises={(size) => {
            resetPaginering(setPage);
            setFilter({ ...filter, antallGjennomforingerVises: size });
          }}
        />
        <Checkbox
          checked={filter.visMineGjennomforinger}
          onChange={(event) => {
            setFilter({
              ...filter,
              visMineGjennomforinger: event.currentTarget.checked,
            });
          }}
        >
          Vis mine gjennomføringer
        </Checkbox>
      </div>
      {tiltaksgjennomforinger.length === 0 ? (
        <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>
      ) : (
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
                    sortable={header.sortable}
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
                        className={styles.title}
                      >
                        <Lenke
                          to={`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`}
                          data-testid="tiltaksgjennomforingrad"
                        >
                          {tiltaksgjennomforing.navn}
                        </Lenke>
                      </Table.DataCell>
                    </SkjulKolonne>

                    <SkjulKolonne skjul={!!skjulKolonner?.enhet}>
                      <Table.DataCell
                        aria-label={`Enheter: ${tiltaksgjennomforing?.navEnheter
                          .map((enhet) => enhet?.navn)
                          .join(", ")}`}
                        title={`Enheter: ${tiltaksgjennomforing?.navEnheter
                          .map((enhet) => enhet?.navn)
                          .join(", ")}`}
                      >
                        {formaterNavEnheter(
                          tiltaksgjennomforing.navRegion,
                          tiltaksgjennomforing.navEnheter,
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
                        aria-label={`Virksomhetsnavn: ${tiltaksgjennomforing.arrangor.navn}`}
                      >
                        {tiltaksgjennomforing.arrangor.navn}
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
                          tiltaksgjennomforing.startDato,
                        )}`}
                        aria-label={`Startdato: ${formaterDato(
                          tiltaksgjennomforing.startDato,
                        )}`}
                      >
                        {formaterDato(tiltaksgjennomforing.startDato)}
                      </Table.DataCell>
                    </SkjulKolonne>

                    <SkjulKolonne skjul={!!skjulKolonner?.sluttdato}>
                      <Table.DataCell
                        title={`Sluttdato ${formaterDato(
                          tiltaksgjennomforing.sluttDato,
                        )}, "-"`}
                        aria-label={
                          tiltaksgjennomforing.sluttDato
                            ? `Sluttdato: ${formaterDato(
                                tiltaksgjennomforing.sluttDato,
                                "-",
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
                  </Table.Row>
                );
              })}
            </Table.Body>
          ) : null}
        </Table>
      )}
      {tiltaksgjennomforinger.length > 0 ? (
        <PagineringContainer>
          <PagineringsOversikt
            page={page}
            antall={tiltaksgjennomforinger.length}
            maksAntall={pagination?.totalCount}
            type="tiltaksgjennomføringer"
            antallVises={filter.antallGjennomforingerVises}
          />
          <Pagination
            className={pageStyles.pagination}
            size="small"
            data-testid="paginering"
            page={page}
            onPageChange={setPage}
            count={Math.ceil(
              (pagination?.totalCount ?? filter.antallGjennomforingerVises) /
                filter.antallGjennomforingerVises,
            )}
            data-version="v1"
          />
        </PagineringContainer>
      ) : null}
    </div>
  );
};
