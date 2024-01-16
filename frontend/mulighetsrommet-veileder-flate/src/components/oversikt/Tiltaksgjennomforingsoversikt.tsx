import { BodyShort, Pagination, Select } from "@navikt/ds-react";
import { useAtom, useAtomValue } from "jotai";
import {
  DelMedBruker,
  TiltaksgjennomforingOppstartstype,
  VeilederflateTiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { paginationAtom, tiltaksgjennomforingsfilter } from "../../core/atoms/atoms";
import { Sorteringsmeny } from "../sorteringmeny/Sorteringsmeny";
import { Gjennomforingsrad } from "./Gjennomforingsrad";
import styles from "./Tiltaksgjennomforingsoversikt.module.scss";
import { useLogEvent } from "../../logging/amplitude";

interface Props {
  tiltaksgjennomforinger: VeilederflateTiltaksgjennomforing[];
  deltMedBruker?: DelMedBruker[];
}

const Tiltaksgjennomforingsoversikt = ({ tiltaksgjennomforinger, deltMedBruker }: Props) => {
  const [pageData, setPages] = useAtom(paginationAtom);
  const filter = useAtomValue(tiltaksgjennomforingsfilter);

  const pagination = (tiltaksgjennomforing: VeilederflateTiltaksgjennomforing[]) => {
    return Math.ceil(tiltaksgjennomforing.length / pageData.pageSize);
  };

  const [sortValue, setSortValue] = useState<string>("tiltakstype-ascending");
  const { logEvent } = useLogEvent();
  useEffect(() => {
    // Reset state
    setPages({ ...pageData, page: 1 });
  }, [filter]);

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

  const antallSize = [15, 50, 100, 1000];
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
      <div className={styles.overskrift_og_sorteringsmeny}>
        <div className={styles.overskrift_og_sorteringsmeny_venstre}>
          {tiltaksgjennomforinger.length > 0 ? (
            <BodyShort>
              Viser {(pageData.page - 1) * pageData.pageSize + 1}-
              {gjennomforingerForSide.length + (pageData.page - 1) * pageData.pageSize} av{" "}
              {tiltaksgjennomforinger.length} tiltak
            </BodyShort>
          ) : null}
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
      </div>
      <ul className={styles.gjennomforinger} data-testid="oversikt_tiltaksgjennomforinger">
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
      <div className={styles.under_oversikt}>
        {tiltaksgjennomforinger.length > 0 ? (
          <>
            <BodyShort>
              Viser {(pageData.page - 1) * pageData.pageSize + 1}-
              {gjennomforingerForSide.length + (pageData.page - 1) * pageData.pageSize} av{" "}
              {tiltaksgjennomforinger.length} tiltak
            </BodyShort>
            <Pagination
              size="small"
              page={pageData.page}
              onPageChange={(page) => setPages({ ...pageData, page })}
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
