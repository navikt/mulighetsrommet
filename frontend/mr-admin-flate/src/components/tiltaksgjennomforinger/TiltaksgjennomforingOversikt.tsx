import { Alert, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { paginationAtom } from "../../api/atoms";
import { PAGE_SIZE } from "../../constants";
import { Laster } from "../laster/Laster";
import styles from "../listeelementer/Listeelementer.module.scss";
import { ListeheaderTiltaksgjennomforinger } from "../listeelementer/Listeheader";
import { PagineringsOversikt } from "../paginering/PagineringOversikt";
import pageStyles from "../../pages/Page.module.scss";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { TiltaksgjennomforingsRad } from "./TiltaksgjennomforingsRad";

export function TiltaksgjennomforingOversikt() {
  const { data, isLoading, isError } = useAdminTiltaksgjennomforinger();
  const [page, setPage] = useAtom(paginationAtom);

  if (!data && isLoading) {
    return (
      <main>
        <Laster size="xlarge" tekst="Laster tiltaksgjennomføringer..." />
      </main>
    );
  }

  if (!data) {
    return <Alert variant="info">Fant ingen tiltaksgjennomføringer</Alert>;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Vi hadde problemer med henting av tiltaksgjennomføringer
      </Alert>
    );
  }

  const { data: tiltaksgjennomforinger, pagination: paginering } = data;

  return (
    <>
      <PagineringsOversikt
        page={page}
        antall={tiltaksgjennomforinger.length}
        maksAntall={paginering.totalCount}
        type="tiltaksgjennomføringer"
      />
      {tiltaksgjennomforinger.length === 0 ? (
        <Alert variant="info">Vi fant ingen tiltaksgjennomføringer</Alert>
      ) : (
        <>
          <ul className={styles.oversikt}>
            <ListeheaderTiltaksgjennomforinger />
            {tiltaksgjennomforinger.map((tiltaksgjennomforing) => (
              <TiltaksgjennomforingsRad
                key={tiltaksgjennomforing.id}
                tiltaksgjennomforing={tiltaksgjennomforing}
              />
            ))}
          </ul>
          <div className={styles.under_oversikt}>
            {tiltaksgjennomforinger.length > 0 && (
              <>
                <PagineringsOversikt
                  page={page}
                  antall={tiltaksgjennomforinger.length}
                  maksAntall={paginering.totalCount}
                  type="tiltaksgjennomføringer"
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
