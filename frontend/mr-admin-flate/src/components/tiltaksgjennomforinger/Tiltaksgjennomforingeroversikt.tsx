import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforing";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";
import { Pagination } from "@navikt/ds-react";
import { paginationAtom } from "mulighetsrommet-veileder-flate/src/core/atoms/atoms";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";

export function Tiltaksgjennomforingeroversikt() {
  const { data, isLoading } = useTiltaksgjennomforing();
  const [page, setPage] = useAtom(paginationAtom);
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
        count={Math.ceil((paginering?.totalCount ?? PAGE_SIZE) / PAGE_SIZE)}
        data-version="v1"
      />
    </>
  );
}
