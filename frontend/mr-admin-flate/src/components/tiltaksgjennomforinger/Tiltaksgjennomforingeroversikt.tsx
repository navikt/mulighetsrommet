import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforinger";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";
import { Heading, Pagination } from "@navikt/ds-react";
import { paginationAtom } from "mulighetsrommet-veileder-flate/src/core/atoms/atoms";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { Alert } from "@navikt/ds-react";

export function Tiltaksgjennomforingeroversikt() {
  const { data, isLoading } = useTiltaksgjennomforinger();
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
        {tiltaksgjennomforinger.length === 0 ? (
          <Alert variant="info">Vi fant ingen tiltaksgjennomføringer</Alert>
        ) : null}
        {tiltaksgjennomforinger.map((tiltaksgjennomforing) => (
          <Tiltaksgjennomforingrad
            key={tiltaksgjennomforing.id}
            tiltaksgjennomforing={tiltaksgjennomforing}
          />
        ))}
      </ul>
      <div className={styles.under_oversikt}>
        {tiltaksgjennomforinger.length > 0 ? (
          <>
            <Heading level="1" size="xsmall" data-testid="antall-tiltak">
              Viser {(page - 1) * PAGE_SIZE + 1}-
              {tiltaksgjennomforinger.length + (page - 1) * PAGE_SIZE} av{" "}
              {paginering?.totalCount} tiltak
            </Heading>
            <Pagination
              size="small"
              data-testid="paginering"
              page={page}
              onPageChange={setPage}
              count={Math.ceil(
                (paginering?.totalCount ?? PAGE_SIZE) / PAGE_SIZE
              )}
              data-version="v1"
            />
          </>
        ) : null}
      </div>
    </>
  );
}
