import { TiltaksgjennomforingFilter } from "@/api/atoms";
import { useAdminTiltaksgjennomforinger } from "@/api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { TabellWrapper } from "@/components/tabell/TabellWrapper";
import {
  createQueryParamsForExcelDownloadForTiltaksgjennomforing,
  formaterDato,
  formaterNavEnheter,
} from "@/utils/Utils";
import { Alert, Pagination, Table, Tag, VStack } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { OpenAPI, SorteringTiltaksgjennomforinger } from "@mr/api-client";
import { TiltaksgjennomforingStatusTag } from "@mr/frontend-common";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";
import React, { createRef, useEffect, useState } from "react";
import pageStyles from "../../pages/Page.module.scss";
import { ShowOpphavValue } from "../debug/ShowOpphavValue";
import { Laster } from "../laster/Laster";
import { PagineringContainer } from "../paginering/PagineringContainer";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import styles from "./Tabell.module.scss";
import { APPLICATION_NAME } from "@/constants";
import { EksporterTabellKnapp } from "@/components/eksporterTabell/EksporterTabellKnapp";

const SkjulKolonne = ({ children, skjul }: { children: React.ReactNode; skjul: boolean }) => {
  return skjul ? null : <>{children}</>;
};

interface Props {
  skjulKolonner?: Partial<Record<Kolonne, boolean>>;
  filterAtom: WritableAtom<TiltaksgjennomforingFilter, TiltaksgjennomforingFilter[], void>;
  tagsHeight: number;
  filterOpen: boolean;
}

export function TiltaksgjennomforingsTabell({
  skjulKolonner,
  filterAtom,
  tagsHeight,
  filterOpen,
}: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const [lasterExcel, setLasterExcel] = useState(false);
  const [excelUrl, setExcelUrl] = useState("");
  const sort = filter.sortering.tableSort;
  const { data, isLoading } = useAdminTiltaksgjennomforinger(filter);
  const link = createRef<HTMLAnchorElement>();

  async function lastNedFil(filter: TiltaksgjennomforingFilter) {
    const headers = new Headers();
    headers.append("Nav-Consumer-Id", APPLICATION_NAME);

    if (import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN) {
      headers.append(
        "Authorization",
        `Bearer ${import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN}`,
      );
    }
    headers.append("accept", "application/json");

    const queryParams = createQueryParamsForExcelDownloadForTiltaksgjennomforing(filter);

    return await fetch(
      `${OpenAPI.BASE}/api/v1/intern/tiltaksgjennomforinger/excel?${queryParams}`,
      {
        headers,
      },
    );
  }

  async function lastNedExcel() {
    setLasterExcel(true);
    if (excelUrl) {
      setExcelUrl("");
    }

    const excelFil = await lastNedFil(filter);
    const blob = await excelFil.blob();
    const url = URL.createObjectURL(blob);
    setExcelUrl(url);
    setLasterExcel(false);
  }

  useEffect(() => {
    if (link.current && excelUrl) {
      link.current.download = "tiltaksgjennomforinger.xlsx";
      link.current.href = excelUrl;

      link.current.click();
      URL.revokeObjectURL(excelUrl);
    }
  }, [excelUrl]);

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

    updateFilter({
      sortering: {
        sortString: `${sortKey}-${direction}` as SorteringTiltaksgjennomforinger,
        tableSort: { orderBy: sortKey, direction },
      },
      page: sort.orderBy !== sortKey || sort.direction !== direction ? 1 : filter.page,
    });
  };

  if (!data || isLoading) {
    return <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />;
  }

  const { pagination, data: tiltaksgjennomforinger } = data;

  return (
    <>
      <ToolbarContainer tagsHeight={tagsHeight} filterOpen={filterOpen}>
        <ToolbarMeny>
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
          <EksporterTabellKnapp lastNedExcel={lastNedExcel} lasterExcel={lasterExcel} />
          <a style={{ display: "none" }} ref={link}></a>
        </ToolbarMeny>
      </ToolbarContainer>
      <TabellWrapper filterOpen={filterOpen}>
        {tiltaksgjennomforinger.length === 0 ? (
          <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>
        ) : (
          <Table
            sort={sort!}
            onSortChange={(sortKey) => handleSort(sortKey!)}
            className={styles.tabell}
            data-testid="tiltaksgjennomforing-tabell"
          >
            <Table.Header
              style={{
                top: `calc(${tagsHeight}px + 6.9rem)`,
              }}
            >
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
                  const formattertSluttDato = tiltaksgjennomforing.sluttDato
                    ? formaterDato(tiltaksgjennomforing.sluttDato)
                    : "-";
                  const formattertStartDato = formaterDato(tiltaksgjennomforing.startDato);

                  return (
                    <Table.Row key={index}>
                      <SkjulKolonne skjul={!!skjulKolonner?.navn}>
                        <Table.DataCell
                          aria-label={`Navn på tiltaksgjennomforing: ${tiltaksgjennomforing.navn}`}
                          className={styles.title}
                        >
                          <VStack>
                            <Lenke
                              to={`${tiltaksgjennomforing?.avtaleId ? `/avtaler/${tiltaksgjennomforing?.avtaleId}/tiltaksgjennomforinger/${tiltaksgjennomforing.id}` : `${tiltaksgjennomforing.id}`} `}
                              data-testid="tiltaksgjennomforing-tabell_tittel"
                            >
                              {tiltaksgjennomforing.navn}
                            </Lenke>
                            <ShowOpphavValue value={tiltaksgjennomforing.opphav} />
                          </VStack>
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
                          title={`Startdato ${formattertStartDato}`}
                          aria-label={`Startdato: ${formattertStartDato}`}
                        >
                          {formattertStartDato}
                        </Table.DataCell>
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.sluttdato}>
                        <Table.DataCell
                          title={`Sluttdato ${formattertSluttDato}`}
                          aria-label={
                            tiltaksgjennomforing.sluttDato
                              ? `Sluttdato ${formattertSluttDato}`
                              : undefined // Noen gjennomføringer har ikke sluttdato så da setter vi heller ikke aria-label for da klager reactA11y
                          }
                        >
                          {formattertSluttDato}
                        </Table.DataCell>
                      </SkjulKolonne>

                      <SkjulKolonne skjul={!!skjulKolonner?.status}>
                        <Table.DataCell>
                          <TiltaksgjennomforingStatusTag status={tiltaksgjennomforing.status} />
                        </Table.DataCell>
                      </SkjulKolonne>
                      <Table.DataCell>
                        <VStack align={"center"}>
                          {tiltaksgjennomforing.publisert ? (
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
      </TabellWrapper>
    </>
  );
}

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
