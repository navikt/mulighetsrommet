import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { useTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforinger";
import styles from "./Tiltaksgjennomforingeroversikt.module.scss";
import { Alert, Heading, Loader, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { paginationAtom } from "../../atoms/atoms";

export function Tiltaksgjennomforingeroversikt() {
  const { data, isLoading } = useTiltaksgjennomforinger();
  const [page, setPage] = useAtom(paginationAtom);
  if (isLoading) {
    return <Loader size="xlarge" />;
  }
  if (!data) {
    return null;
  }
  const { data: tiltaksgjennomforinger, pagination: paginering } = data;

  const PagineringsOversikt = () => {
    return (
      <Heading level="1" size="xsmall" data-testid="antall-tiltak">
        Viser {(page - 1) * PAGE_SIZE + 1}-
        {tiltaksgjennomforinger.length + (page - 1) * PAGE_SIZE} av
        {paginering?.totalCount} tiltak
      </Heading>
    );
  };

  return (
    <>
      {tiltaksgjennomforinger.length > 0 ? <PagineringsOversikt /> : null}

      <ul className={styles.oversikt}>
        {tiltaksgjennomforinger.length === 0 && (
          <Alert variant="info">Vi fant ingen tiltaksgjennomføringer</Alert>
        )}
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
            <PagineringsOversikt />
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
