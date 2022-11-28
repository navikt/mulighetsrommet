import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforing";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";
import { Pagination } from "@navikt/ds-react";
import { paginationAtom } from "mulighetsrommet-veileder-flate/src/core/atoms/atoms";
import { useAtom } from "jotai";
import { useEffect, useState } from "react";
import { UseQueryResult } from "@tanstack/react-query";
import { PaginertTiltaksgjennomforing } from "../../../../mulighetsrommet-api-client";

export function Tiltaksgjennomforingeroversikt() {
  const { data, isLoading } = useTiltaksgjennomforing();
  const [page, setPage] = useAtom(paginationAtom);
  /*const [queryResult, setData] =
    useState<UseQueryResult<PaginertTiltaksgjennomforing>>();
  setData(useTiltaksgjennomforing(page));*/
  /*useEffect(() => {
    const fetchData = async () => {
      const data = await useTiltaksgjennomforing(page);
      setData(data);
    };
    fetchData();
  }, [page]);
*/
  if (isLoading) {
    return null;
  }
  if (!data) {
    return null;
  }
  const { data: tiltaksgjennomforinger, pagination: paginering } = data;

  return (
    <>
      <ul className={styles.oversikt}>
        {tiltaksgjennomforinger.map((tiltaksgjennomforing) => (
          <Tiltaksgjennomforingrad
            key={tiltaksgjennomforing.id}
            tiltaksgjennomforing={tiltaksgjennomforing}
          />
        ))}
      </ul>
      <Pagination
        size="small"
        data-testid="paginering"
        page={page}
        onPageChange={setPage}
        count={25}
        data-version="v1"
      />
    </>
  );
}
