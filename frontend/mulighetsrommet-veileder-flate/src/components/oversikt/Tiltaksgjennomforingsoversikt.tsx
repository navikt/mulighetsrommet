import { paginationAtom } from "@/core/atoms";
import { useArbeidsmarkedstiltakFilterValue } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { useLogEvent } from "@/logging/amplitude";
import { BodyShort, Pagination, Select } from "@navikt/ds-react";
import classnames from "classnames";
import { useAtom } from "jotai";
import {
  DelMedBruker,
  TiltaksgjennomforingOppstartstype,
  VeilederflateTiltaksgjennomforing,
} from "@mr/api-client";
import { ReactNode, useEffect } from "react";
import { Sorteringsmeny } from "../sorteringmeny/Sorteringsmeny";
import { Gjennomforingsrad } from "./Gjennomforingsrad";
import styles from "./Tiltaksgjennomforingsoversikt.module.scss";
import { sorteringAtom } from "../sorteringmeny/sorteringAtom";
import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";

interface Props {
  tiltaksgjennomforinger: VeilederflateTiltaksgjennomforing[];
  deltMedBruker?: DelMedBruker[];
  varsler?: React.ReactNode;
  filterOpen: boolean;
  feilmelding: ReactNode;
  tagsHeight: number;
}

export function Tiltaksgjennomforingsoversikt({
  tiltaksgjennomforinger,
  deltMedBruker,
  varsler,
  filterOpen,
  feilmelding,
  tagsHeight,
}: Props) {
  const [pageData, setPages] = useAtom(paginationAtom);
  const filter = useArbeidsmarkedstiltakFilterValue();
  const pagination = (tiltaksgjennomforing: VeilederflateTiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / pageData.pageSize);
  };

  const ViserAntallTiltakTekst = () => {
    return tiltaksgjennomforinger.length > 0 ? (
      <BodyShort>
        Viser {(pageData.page - 1) * pageData.pageSize + 1}-
        {gjennomforingerForSide.length + (pageData.page - 1) * pageData.pageSize} av{" "}
        {tiltaksgjennomforinger.length} tiltak
      </BodyShort>
    ) : (
      <BodyShort>Viser 0 tiltak</BodyShort>
    );
  };

  const [sortValue, setSortValue] = useAtom(sorteringAtom);

  const { logEvent } = useLogEvent();
  useEffect(() => {
    // Reset state
    setPages({ ...pageData, page: 1 });
  }, [filter, sortValue]);

  const getSort = (
    sortValue: string,
  ): {
    direction: "ascending" | "descending";
    orderBy: keyof VeilederflateTiltaksgjennomforing;
  } => {
    const [orderBy, direction] = sortValue.split("-");
    return {
      orderBy: orderBy as keyof VeilederflateTiltaksgjennomforing,
      direction: direction as "ascending" | "descending",
    };
  };

  const sorter = (
    tiltaksgjennomforinger: VeilederflateTiltaksgjennomforing[],
  ): VeilederflateTiltaksgjennomforing[] => {
    return tiltaksgjennomforinger.sort((a, b) => {
      const sort = getSort(sortValue);
      const comparator = (
        a: VeilederflateTiltaksgjennomforing,
        b: VeilederflateTiltaksgjennomforing,
        orderBy: keyof VeilederflateTiltaksgjennomforing,
      ) => {
        const compare = (item1: any, item2: any) => {
          if (item2 < item1 || item2 === undefined) return 1;
          if (item2 > item1) return -1;
          return 0;
        };

        if (orderBy === "oppstart") {
          const dateB =
            b.oppstart === TiltaksgjennomforingOppstartstype.FELLES
              ? new Date(b.oppstartsdato!) // Oppstartsdato skal alltid være tilgjengelig når oppstartstype er FELLES
              : new Date();
          const dateA =
            a.oppstart === TiltaksgjennomforingOppstartstype.FELLES
              ? new Date(a.oppstartsdato!) // Oppstartsdato skal alltid være tilgjengelig når oppstartstype er FELLES
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
  };

  const antallSize = [50, 100, 1000];
  const lopendeGjennomforinger = tiltaksgjennomforinger.filter(
    (gj) => gj.oppstart === TiltaksgjennomforingOppstartstype.LOPENDE,
  );
  const gjennomforingerMedFellesOppstart = tiltaksgjennomforinger.filter(
    (gj) => gj.oppstart !== TiltaksgjennomforingOppstartstype.LOPENDE,
  );

  const gjennomforingerForSide = (
    getSort(sortValue).orderBy === "oppstart"
      ? [...sorter(gjennomforingerMedFellesOppstart), ...lopendeGjennomforinger]
      : sorter(tiltaksgjennomforinger)
  ).slice((pageData.page - 1) * pageData.pageSize, pageData.page * pageData.pageSize);

  return (
    <>
      <ToolbarContainer tagsHeight={tagsHeight} filterOpen={filterOpen}>
        {varsler}
        {gjennomforingerForSide.length > 0 ? (
          <ToolbarMeny>
            <div className={styles.visningsmeny}>
              <ViserAntallTiltakTekst />
              <Select
                size="small"
                label="Velg antall"
                hideLabel
                name="size"
                value={pageData.pageSize}
                onChange={(e) => {
                  setPages({ page: 1, pageSize: parseInt(e.currentTarget.value) });
                  logEvent({
                    name: "arbeidsmarkedstiltak.vis-antall-tiltak",
                    data: {
                      valgt_antall: parseInt(e.currentTarget.value),
                      antall_tiltak: tiltaksgjennomforinger.length,
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
          const delMedBruker = deltMedBruker?.find((delt) => {
            return (
              (delt.tiltaksgjennomforingId && delt.tiltaksgjennomforingId === gjennomforing.id) ||
              (delt.sanityId && delt.sanityId === gjennomforing.sanityId)
            );
          });
          return (
            <Gjennomforingsrad
              key={gjennomforing.id ?? gjennomforing.sanityId}
              index={index}
              tiltaksgjennomforing={gjennomforing}
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
          <ViserAntallTiltakTekst />
          <Pagination
            size="small"
            page={pageData.page}
            onPageChange={(page) => setPages({ ...pageData, page })}
            count={
              pagination(tiltaksgjennomforinger) === 0 ? 1 : pagination(tiltaksgjennomforinger)
            }
            data-version="v1"
          />
        </div>
      ) : null}
    </>
  );
}
