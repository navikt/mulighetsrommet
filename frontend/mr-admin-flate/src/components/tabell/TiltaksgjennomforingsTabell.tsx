import { Alert, Checkbox, Pagination, Table, Tag, VStack } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { SorteringTiltaksgjennomforinger } from "mulighetsrommet-api-client";
import { Lenke } from "../../../../frontend-common/components/lenke/Lenke";
import React from "react";
import { TiltaksgjennomforingFilter } from "../../api/atoms";
import { useSort } from "../../hooks/useSort";
import pageStyles from "../../pages/Page.module.scss";
import { formaterDato, formaterNavEnheter } from "../../utils/Utils";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { TiltaksgjennomforingstatusTag } from "../statuselementer/TiltaksgjennomforingstatusTag";
import styles from "./Tabell.module.scss";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";

interface ColumnHeader {
  sortKey: Kolonne;
  tittel: string;
  sortable: boolean;
  width: string;
}

const headers: ColumnHeader[] = [
  {
    sortKey: "navn",
    tittel: "Tiltaksnavn",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "enhet",
    tittel: "Enhet",
    sortable: false,
    width: "2fr",
  },
  {
    sortKey: "tiltaksnummer",
    tittel: "Tiltaksnr.",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "arrangor",
    tittel: "Arrangør",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "tiltakstype",
    tittel: "Tiltakstype",
    sortable: true,
    width: "3fr",
  },
  {
    sortKey: "startdato",
    tittel: "Startdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "sluttdato",
    tittel: "Sluttdato",
    sortable: true,
    width: "1fr",
  },
  {
    sortKey: "status",
    tittel: "Status",
    sortable: false,
    width: "1fr",
  },
  {
    sortKey: "publisert",
    tittel: "Publisert",
    sortable: true,
    width: "1fr",
  },
];

type Kolonne =
  | "dupliser"
  | "navn"
  | "enhet"
  | "tiltaksnummer"
  | "tiltakstype"
  | "arrangor"
  | "startdato"
  | "sluttdato"
  | "status"
  | "publisert";

interface Props {
  skjulKolonner?: Partial<Record<Kolonne, boolean>>;
  filterAtom: WritableAtom<TiltaksgjennomforingFilter, TiltaksgjennomforingFilter[], void>;
}

const SkjulKolonne = ({ children, skjul }: { children: React.ReactNode; skjul: boolean }) => {
  return skjul ? null : <>{children}</>;
};

export const TiltaksgjennomforingsTabell = ({ skjulKolonner, filterAtom }: Props) => {
  const [sort, setSort] = useSort("navn");
  const [filter, setFilter] = useAtom(filterAtom);

  const { data, isLoading } = useAdminTiltaksgjennomforinger(filter);

  function updateFilter(newFilter: Partial<TiltaksgjennomforingFilter>) {
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
      sortering: `${sortKey}-${direction}` as SorteringTiltaksgjennomforinger,
      page: sort.orderBy !== sortKey || sort.direction !== direction ? 1 : filter.page,
    });
  };

  if (!data || isLoading) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  const { pagination, data: tiltaksgjennomforinger } = data;

  return (
    <div className={styles.tabell_wrapper}>
      <div className={styles.flex}>
        <PagineringsOversikt
          page={filter.page}
          pageSize={filter.pageSize}
          antall={tiltaksgjennomforinger.length}
          maksAntall={pagination.totalCount}
          type="tiltaksgjennomføringer"
          onChangePageSize={(size) => {
            updateFilter({
              page: 1,
              pageSize: size,
            });
          }}
        />
        <Checkbox
          checked={filter.visMineGjennomforinger}
          onChange={(event) => {
            updateFilter({
              page: 1,
              visMineGjennomforinger: event.currentTarget.checked,
            });
          }}
        >
          Vis kun mine
        </Checkbox>
      </div>
      {tiltaksgjennomforinger.length === 0 ? (
        <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>
      ) : (
        <Table
          sort={sort!}
          onSortChange={(sortKey) => handleSort(sortKey!)}
          className={styles.tabell}
          data-testid="tiltaksgjennomforing-tabell"
        >
          <Table.Header>
            <Table.Row>
              {headers
                .filter((header) => {
                  return skjulKolonner ? !skjulKolonner[header.sortKey] : true;
                })
                .map((header) => (
                  <Table.ColumnHeader
                    key={header.sortKey}
                    sortKey={header.sortKey}
                    sortable={header.sortable}
                    style={{
                      width: header.width,
                    }}
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
                  <Table.Row key={index}>
                    <SkjulKolonne skjul={!!skjulKolonner?.navn}>
                      <Table.DataCell
                        aria-label={`Navn på tiltaksgjennomforing: ${tiltaksgjennomforing.navn}`}
                        className={styles.title}
                      >
                        <Lenke
                          to={`${tiltaksgjennomforing.id}`}
                          data-testid="tiltaksgjennomforing-tabell_tittel"
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
                          tiltaksgjennomforing.navRegion?.navn,
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
                        title={`Startdato ${formaterDato(tiltaksgjennomforing.startDato)}`}
                        aria-label={`Startdato: ${formaterDato(tiltaksgjennomforing.startDato)}`}
                      >
                        {formaterDato(tiltaksgjennomforing.startDato)}
                      </Table.DataCell>
                    </SkjulKolonne>

                    <SkjulKolonne skjul={!!skjulKolonner?.sluttdato}>
                      <Table.DataCell
                        title={`Sluttdato ${formaterDato(tiltaksgjennomforing.sluttDato)}, "-"`}
                        aria-label={
                          tiltaksgjennomforing.sluttDato
                            ? `Sluttdato: ${formaterDato(tiltaksgjennomforing.sluttDato, "-")}`
                            : undefined // Noen gjennomføringer har ikke sluttdato så da setter vi heller ikke aria-label for da klager reactA11y
                        }
                      >
                        {formaterDato(tiltaksgjennomforing.sluttDato)}
                      </Table.DataCell>
                    </SkjulKolonne>

                    <SkjulKolonne skjul={!!skjulKolonner?.status}>
                      <Table.DataCell>
                        <TiltaksgjennomforingstatusTag
                          tiltaksgjennomforing={tiltaksgjennomforing}
                        />
                      </Table.DataCell>
                    </SkjulKolonne>
                    <Table.DataCell>
                      <VStack align={"center"}>
                        {tiltaksgjennomforing.publisertForAlle ? (
                          <Tag
                            aria-label="Tiltaket er publisert for alle"
                            title="Tiltaket er publisert for alle"
                            variant="success-filled"
                          >
                            Ja
                          </Tag>
                        ) : (
                          <span
                            // Denne span'en må være her så brukere av skjermlesere får beskjed om at tiltaket ikke er tilgjengelig.
                            // Klassen under gjør at elementet er usynlig for brukere som kan se, men skjermlesere kan fortsatt få tak i elementet
                            className="navds-sr-only"
                            title="Tiltaket er ikke publisert for alle"
                          />
                        )}
                      </VStack>
                    </Table.DataCell>
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
            page={filter.page}
            pageSize={filter.pageSize}
            antall={tiltaksgjennomforinger.length}
            maksAntall={pagination.totalCount}
            type="tiltaksgjennomføringer"
          />
          <Pagination
            className={pageStyles.pagination}
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
