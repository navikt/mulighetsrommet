import { BodyShort, Pagination } from "@navikt/ds-react";
import { useAtom, useAtomValue } from "jotai";
import {
  HarDeltMedBruker,
  TiltaksgjennomforingOppstartstype,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { paginationAtom, tiltaksgjennomforingsfilter } from "../../core/atoms/atoms";
import { Sorteringsmeny } from "../sorteringmeny/Sorteringsmeny";
import { Gjennomforingsrad } from "./Gjennomforingsrad";
import styles from "./Tiltaksgjennomforingsoversikt.module.scss";
import { useHentAlleTiltakDeltMedBruker } from "../../core/api/queries/useHentAlleTiltakDeltMedBruker";
import { useAppContext } from "../../hooks/useAppContext";

interface Props {
  tiltaksgjennomforinger: VeilederflateTiltaksgjennomforing[];
  isFetching: boolean;
}

const Tiltaksgjennomforingsoversikt = (props: Props) => {
  const { tiltaksgjennomforinger, isFetching } = props;
  const { fnr } = useAppContext();
  const { alleTiltakDeltMedBruker } = useHentAlleTiltakDeltMedBruker(fnr);
  const filter = useAtomValue(tiltaksgjennomforingsfilter);

  const [page, setPage] = useAtom(paginationAtom);
  const elementsPerPage = 15;
  const pagination = (tiltaksgjennomforing: VeilederflateTiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / elementsPerPage);
  };

  const [sortValue, setSortValue] = useState<string>("tiltakstype-ascending");

  useEffect(() => {
    if (tiltaksgjennomforinger.length <= elementsPerPage && !isFetching) {
      // Reset state
      setPage(1);
    }
  }, [tiltaksgjennomforinger]);

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
              ? new Date(b.oppstartsdato!!) // Oppstartsdato skal alltid være tilgjengelig når oppstartstype er FELLES
              : new Date();
          const dateA =
            a.oppstart === TiltaksgjennomforingOppstartstype.FELLES
              ? new Date(a.oppstartsdato!!) // Oppstartsdato skal alltid være tilgjengelig når oppstartstype er FELLES
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

  const lopendeGjennomforinger = tiltaksgjennomforinger.filter(
    (gj) => gj.oppstart === TiltaksgjennomforingOppstartstype.LOPENDE,
  );
  const gjennomforingerMedFellesOppstart = tiltaksgjennomforinger.filter(
    (gj) => gj.oppstart !== TiltaksgjennomforingOppstartstype.LOPENDE,
  );

  const gjennomforingerForSide = (
    getSort(sortValue).orderBy === "oppstart"
      ? [...sorter(gjennomforingerMedFellesOppstart), ...lopendeGjennomforinger]
      : sorter(tiltaksgjennomforinger).filter((gjennomforing) => {
          const iderForDelteTiltak = alleTiltakDeltMedBruker
            ?.map((delt) => delt.tiltaksgjennomforingId || delt.sanityId)
            .filter(Boolean);
          if (filter.harDeltMedBruker === HarDeltMedBruker.HAR_DELT) {
            return (
              iderForDelteTiltak?.includes(gjennomforing.id) ||
              iderForDelteTiltak?.includes(gjennomforing.sanityId)
            );
          } else if (filter.harDeltMedBruker === HarDeltMedBruker.HAR_IKKE_DELT) {
            return (
              !iderForDelteTiltak?.includes(gjennomforing.id) &&
              !iderForDelteTiltak?.includes(gjennomforing.sanityId)
            );
          } else if (filter.harDeltMedBruker === HarDeltMedBruker.HAR_ELLER_HAR_IKKE_DELT) {
            return true;
          }
        })
  ).slice((page - 1) * elementsPerPage, page * elementsPerPage);

  return (
    <>
      <div className={styles.overskrift_og_sorteringsmeny}>
        {tiltaksgjennomforinger.length > 0 ? (
          <BodyShort>
            Viser {(page - 1) * elementsPerPage + 1}-
            {gjennomforingerForSide.length + (page - 1) * elementsPerPage} av{" "}
            {tiltaksgjennomforinger.length} tiltak
          </BodyShort>
        ) : null}
        <Sorteringsmeny sortValue={sortValue} setSortValue={setSortValue} />
      </div>
      <ul className={styles.gjennomforinger} data-testid="oversikt_tiltaksgjennomforinger">
        {gjennomforingerForSide.map((gjennomforing, index) => {
          const deltMedBruker = alleTiltakDeltMedBruker?.find((delt) => {
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
              deltMedBruker={deltMedBruker}
            />
          );
        })}
      </ul>
      <div className={styles.under_oversikt}>
        {tiltaksgjennomforinger.length > 0 ? (
          <>
            <BodyShort>
              Viser {(page - 1) * elementsPerPage + 1}-
              {gjennomforingerForSide.length + (page - 1) * elementsPerPage} av{" "}
              {tiltaksgjennomforinger.length} tiltak
            </BodyShort>
            <Pagination
              size="small"
              page={page}
              onPageChange={setPage}
              count={
                pagination(tiltaksgjennomforinger) === 0 ? 1 : pagination(tiltaksgjennomforinger)
              }
              data-version="v1"
            />
          </>
        ) : null}
      </div>
    </>
  );
};

export default Tiltaksgjennomforingsoversikt;
