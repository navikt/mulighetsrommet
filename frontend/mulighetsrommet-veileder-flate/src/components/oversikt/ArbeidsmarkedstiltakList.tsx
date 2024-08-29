import { paginationAtom } from "@/core/atoms";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { useLogEvent } from "@/logging/amplitude";
import { BodyShort, Pagination, Select } from "@navikt/ds-react";
import classnames from "classnames";
import { useAtom } from "jotai";
import {
  DelMedBruker,
  TiltaksgjennomforingOppstartstype,
  VeilederflateTiltak,
} from "@mr/api-client";
import { ReactNode, useEffect } from "react";
import { Sorteringsmeny } from "../sorteringmeny/Sorteringsmeny";
import { ArbeidsmarkedstiltakListItem } from "./ArbeidsmarkedstiltakListItem";
import styles from "./ArbeidsmarkedstiltakList.module.scss";
import { sorteringAtom } from "../sorteringmeny/sorteringAtom";
import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  tiltak: VeilederflateTiltak[];
  deltMedBruker?: DelMedBruker[];
  varsler?: ReactNode;
  filterOpen: boolean;
  feilmelding: ReactNode;
  tagsHeight: number;
}

export function ArbeidsmarkedstiltakList({
  tiltak,
  deltMedBruker,
  varsler,
  filterOpen,
  feilmelding,
  tagsHeight,
}: Props) {
  const [{ page, pageSize }, setPages] = useAtom(paginationAtom);
  const filter = useArbeidsmarkedstiltakFilterValue();
  const [sortValue, setSortValue] = useAtom(sorteringAtom);

  const { logEvent } = useLogEvent();

  useEffect(() => {
    // Reset state
    setPages({ pageSize, page: 1 });
  }, [filter, pageSize, setPages, sortValue]);

  const antallSize = [10, 50, 100, 1000];
  const lopendeGjennomforinger = tiltak.filter(
    (gj) => gj.oppstart === TiltaksgjennomforingOppstartstype.LOPENDE,
  );
  const gjennomforingerMedFellesOppstart = tiltak.filter(
    (gj) => gj.oppstart !== TiltaksgjennomforingOppstartstype.LOPENDE,
  );

  const sort = getSort(sortValue);

  const gjennomforingerForSide = (
    sort.orderBy === "oppstart"
      ? [...sorter(gjennomforingerMedFellesOppstart, sort), ...lopendeGjennomforinger]
      : sorter(tiltak, sort)
  ).slice((page - 1) * pageSize, page * pageSize);

  return (
    <>
      <ToolbarContainer tagsHeight={tagsHeight} filterOpen={filterOpen}>
        {varsler}
        {gjennomforingerForSide.length > 0 ? (
          <ToolbarMeny>
            <div className={styles.visningsmeny}>
              <AntallTiltakSummary
                totaltAntallTiltak={tiltak.length}
                antallTiltak={gjennomforingerForSide.length}
                page={page}
                pageSize={pageSize}
              />
              <Select
                size="small"
                label="Velg antall"
                hideLabel
                name="size"
                value={pageSize}
                onChange={(e) => {
                  setPages({ page: 1, pageSize: parseInt(e.currentTarget.value) });
                  logEvent({
                    name: "arbeidsmarkedstiltak.vis-antall-tiltak",
                    data: {
                      valgt_antall: parseInt(e.currentTarget.value),
                      antall_tiltak: tiltak.length,
                    },
                  });
                }}
              >
                {antallSize.map((ant) => (
                  <option key={ant} value={ant}>
                    {ant}
                  </option>
                ))}
              </Select>
            </div>
            <Sorteringsmeny sortValue={sortValue} setSortValue={setSortValue} />
          </ToolbarMeny>
        ) : null}
      </ToolbarContainer>
      {feilmelding}
      <ul
        className={classnames(
          styles.gjennomforinger,
          filterOpen && styles.gjennomforinger_filter_open,
        )}
        data-testid="oversikt_tiltaksgjennomforinger"
      >
        {gjennomforingerForSide.map((gjennomforing, index) => {
          const id = isTiltakGruppe(gjennomforing) ? gjennomforing.id : gjennomforing.sanityId;
          const delMedBruker = deltMedBruker?.find((delt) => {
            return delt.tiltaksgjennomforingId === id || delt.sanityId === id;
          });
          return (
            <ArbeidsmarkedstiltakListItem
              key={id}
              index={index}
              tiltak={gjennomforing}
              delMedBruker={delMedBruker}
            />
          );
        })}
      </ul>
      {gjennomforingerForSide.length > 0 ? (
        <div
          className={classnames(
            styles.under_oversikt,
            filterOpen && styles.under_oversikt_filter_open,
          )}
        >
          <AntallTiltakSummary
            totaltAntallTiltak={tiltak.length}
            antallTiltak={gjennomforingerForSide.length}
            page={page}
            pageSize={pageSize}
          />
          <Pagination
            size="small"
            page={page}
            onPageChange={(page) => setPages({ pageSize, page })}
            count={getTotalPages(tiltak, pageSize)}
            data-version="v1"
          />
        </div>
      ) : null}
    </>
  );
}

function AntallTiltakSummary({
  totaltAntallTiltak,
  antallTiltak,
  page,
  pageSize,
}: {
  totaltAntallTiltak: number;
  antallTiltak: number;
  page: number;
  pageSize: number;
}) {
  const index = (page - 1) * pageSize;
  return totaltAntallTiltak > 0 ? (
    <BodyShort>
      Viser {index + 1}-{index + antallTiltak} av {totaltAntallTiltak} tiltak
    </BodyShort>
  ) : (
    <BodyShort>Viser 0 tiltak</BodyShort>
  );
}

function getTotalPages(tiltak: VeilederflateTiltak[], pageSize: number) {
  const totalPages = Math.ceil(tiltak.length / pageSize);
  return totalPages === 0 ? 1 : totalPages;
}

function getSort(sortValue: string): {
  direction: "ascending" | "descending";
  orderBy: keyof VeilederflateTiltak;
} {
  const [orderBy, direction] = sortValue.split("-");
  return {
    orderBy: orderBy as keyof VeilederflateTiltak,
    direction: direction as "ascending" | "descending",
  };
}

function sorter(
  tiltak: VeilederflateTiltak[],
  sort: { orderBy: keyof VeilederflateTiltak; direction: string },
): VeilederflateTiltak[] {
  return tiltak.sort((a, b) => {
    const comparator = (
      a: VeilederflateTiltak,
      b: VeilederflateTiltak,
      orderBy: keyof VeilederflateTiltak,
    ) => {
      const compare = (item1: any, item2: any) => {
        if (item2 < item1 || item2 === undefined) return 1;
        if (item2 > item1) return -1;
        return 0;
      };

      if (orderBy === "oppstart") {
        const dateB =
          b.oppstart === TiltaksgjennomforingOppstartstype.FELLES
            ? new Date(b.oppstartsdato)
            : new Date();
        const dateA =
          a.oppstart === TiltaksgjennomforingOppstartstype.FELLES
            ? new Date(a.oppstartsdato)
            : new Date();
        return compare(dateA, dateB);
      } else if (orderBy === "tiltakstype") {
        return compare(a.tiltakstype.navn, b.tiltakstype.navn);
      } else {
        return compare(a[orderBy], b[orderBy]);
      }
    };

    return sort.direction === "ascending"
      ? comparator(a, b, sort.orderBy)
      : comparator(b, a, sort.orderBy);
  });
}
