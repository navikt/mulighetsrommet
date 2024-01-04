import { BodyShort, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  TiltaksgjennomforingOppstartstype,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { paginationAtom } from "../../core/atoms/atoms";
import { Sorteringsmeny } from "../sorteringmeny/Sorteringsmeny";
import { Gjennomforingsrad } from "./Gjennomforingsrad";
import styles from "./Tiltaksgjennomforingsoversikt.module.scss";

interface Props {
  tiltaksgjennomforinger: VeilederflateTiltaksgjennomforing[];
  isFetching: boolean;
}

const Tiltaksgjennomforingsoversikt = (props: Props) => {
  const { tiltaksgjennomforinger, isFetching } = props;

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

  const gjennomforingerForSide = sorter(tiltaksgjennomforinger).slice(
    (page - 1) * elementsPerPage,
    page * elementsPerPage,
  );

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
          return (
            <Gjennomforingsrad
              key={gjennomforing.id ?? gjennomforing.sanityId}
              index={index}
              tiltaksgjennomforing={gjennomforing}
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
