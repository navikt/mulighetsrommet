import { paginationAtom } from "@/core/atoms";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { BodyShort, Pagination, Select } from "@navikt/ds-react";
import classnames from "classnames";
import { useAtom } from "jotai";
import { DelMedBrukerDto, GjennomforingOppstartstype, VeilederflateTiltak } from "@api-client";
import { ReactNode, useEffect } from "react";
import { Sorteringsmeny } from "../sorteringmeny/Sorteringsmeny";
import { ArbeidsmarkedstiltakListItem } from "./ArbeidsmarkedstiltakListItem";
import { sorteringAtom } from "../sorteringmeny/sorteringAtom";
import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  tiltak: VeilederflateTiltak[];
  deltMedBruker?: DelMedBrukerDto[];
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

  useEffect(() => {
    // Reset state
    setPages({ pageSize, page: 1 });
  }, [filter, pageSize, setPages, sortValue]);

  const antallSize = [10, 50, 100, 1000];
  const lopendeGjennomforinger = tiltak.filter(
    (gj) => gj.oppstart === GjennomforingOppstartstype.LOPENDE,
  );
  const gjennomforingerMedFellesOppstart = tiltak.filter(
    (gj) => gj.oppstart !== GjennomforingOppstartstype.LOPENDE,
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
            <div className="flex gap-8 items-center">
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
        className={classnames("m-0 mb-4 flex flex-col gap-3 p-0", filterOpen && "xl:pl-2 pl-4")}
        data-testid="oversikt_gjennomforinger"
      >
        {gjennomforingerForSide.map((gjennomforing, index) => {
          const id = isTiltakGruppe(gjennomforing) ? gjennomforing.id : gjennomforing.sanityId;
          const delMedBruker = deltMedBruker?.find((delt) => {
            return delt.gjennomforingId === id || delt.sanityId === id;
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
        <div className={classnames("flex justify-start mt-4 mb-8", filterOpen && "pl-4")}>
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
          b.oppstart === GjennomforingOppstartstype.FELLES && "oppstartsdato" in b
            ? new Date(b.oppstartsdato)
            : new Date();
        const dateA =
          a.oppstart === GjennomforingOppstartstype.FELLES && "oppstartsdato" in a
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
