import { Alert, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { paginationAtom } from "../../api/atoms";
import { PAGE_SIZE } from "../../constants";
import { Laster } from "../laster/Laster";
import styles from "../listeelementer/Listeelementer.module.scss";
import { ListeheaderTiltakstyper } from "../listeelementer/Listeheader";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import { TiltakstypeRad } from "./TiltakstypeRad";
import pageStyles from "../../pages/Page.module.scss";
import useTiltakstyperWithFilter from "../../api/tiltakstyper/useTiltakstyperWithFilter";

export function TiltakstyperOversikt() {
  const [page, setPage] = useAtom(paginationAtom);
  const { data, isLoading, isError } = useTiltakstyperWithFilter();

  if (!data && isLoading) {
    return <Laster size="xlarge" tekst="Laster tiltakstyper..." />;
  }

  if (!data) {
    return <Alert variant="info">Fant ingen tiltakstyper</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Vi hadde problemer med henting av tiltakstyper
      </Alert>
    );
  }
  const { data: tiltakstyper, pagination: paginering } = data;

  return (
    <>
      <PagineringsOversikt
        page={page}
        antall={tiltakstyper.length}
        maksAntall={paginering.totalCount}
        type="tiltakstyper"
      />
      {tiltakstyper.length === 0 ? (
        <Alert variant="info">Vi fant ingen tiltakstyper</Alert>
      ) : (
        <>
          <ul className={styles.oversikt}>
            <ListeheaderTiltakstyper />
            {tiltakstyper.map((tiltakstype) => (
              <TiltakstypeRad key={tiltakstype.id} tiltakstype={tiltakstype} />
            ))}
          </ul>
          <div className={styles.under_oversikt}>
            {tiltakstyper.length > 0 && (
              <>
                <PagineringsOversikt
                  page={page}
                  antall={tiltakstyper.length}
                  maksAntall={paginering.totalCount}
                  type="tiltakstyper"
                />
                <Pagination
                  className={pageStyles.pagination}
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
            )}
          </div>
        </>
      )}
    </>
  );
}
