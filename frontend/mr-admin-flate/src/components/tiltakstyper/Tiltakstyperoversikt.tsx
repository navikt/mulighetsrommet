import styles from "./Tiltakstyperoversikt.module.scss";
import { Alert, Heading, Pagination } from "@navikt/ds-react";
import { paginationAtom } from "mulighetsrommet-veileder-flate/src/core/atoms/atoms";
import { useAtom } from "jotai";
import { PAGE_SIZE } from "../../constants";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { Tiltakstyperad } from "./Tiltakstyperad";

export function Tiltakstyperoversikt() {
  const { data, isLoading } = useTiltakstyper();
  const [page, setPage] = useAtom(paginationAtom);
  if (isLoading) {
    return null;
  }
  if (!data) {
    return null;
  }
  const { data: tiltakstyper, pagination: paginering } = data;

  const PagineringsOversikt = () => {
    return (
      <Heading level="1" size="xsmall" data-testid="antall-tiltak">
        Viser {(page - 1) * PAGE_SIZE + 1}-
        {tiltakstyper.length + (page - 1) * PAGE_SIZE} av{" "}
        {paginering?.totalCount} tiltak
      </Heading>
    );
  };

  return (
    <>
      {tiltakstyper.length > 0 ? <PagineringsOversikt /> : null}
      <ul className={styles.oversikt}>
        {tiltakstyper.length === 0 ? (
          <Alert variant="info">Vi fant ingen tiltakstyper</Alert>
        ) : null}
        {tiltakstyper.map((tiltakstype) => (
          <Tiltakstyperad key={tiltakstype.id} tiltakstype={tiltakstype} />
        ))}
      </ul>
      <div className={styles.under_oversikt}>
        {tiltakstyper.length > 0 ? (
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
