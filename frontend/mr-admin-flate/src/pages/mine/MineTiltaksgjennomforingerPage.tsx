import { Alert, BodyLong, Heading, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { paginationAtom } from "../../api/atoms";
import { useTiltaksgjennomforingerByInnloggetAnsatt } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByInnloggetAnsatt";
import { Laster } from "../../components/Laster";
import { PagineringsOversikt } from "../../components/paginering/PagineringOversikt";
import { SokEtterTiltaksgjennomforing } from "../../components/sok/SokEtterTiltaksgjennomforing";
import { Tiltaksgjennomforingrad } from "../../components/tiltaksgjennomforinger/Tiltaksgjennomforing";
import { PAGE_SIZE } from "../../constants";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";

export function MineTiltaksgjennomforingerPage() {
  const { data, isFetching, isError } =
    useTiltaksgjennomforingerByInnloggetAnsatt();
  const [page, setPage] = useAtom(paginationAtom);

  if (isFetching) {
    return <Laster />;
  }

  if (isError) {
    return (
      <Alert variant="error">
        <p>
          Det oppsto en feil ved henting av dine tiltaksgjennomføringer. Prøv
          igjen senere.
        </p>
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  if (!data) {
    return (
      <Alert variant="warning">
        <p>Klarte ikke finne dine tiltaksgjennomføringer</p>
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  const tiltaksgjennomforinger = data.data;
  return (
    <>
      <Link to="/">Hjem</Link>
      <Heading className={styles.overskrift} size="large">
        Oversikt over mine tiltaksgjennomføringer
      </Heading>
      <BodyLong className={styles.body} size="small">
        Her finner du gjennomføringer du har lagt til i din liste
      </BodyLong>
      <SokEtterTiltaksgjennomforing />
      <>
        {tiltaksgjennomforinger.length > 0 ? (
          <PagineringsOversikt
            page={page}
            antall={tiltaksgjennomforinger.length}
            maksAntall={data.pagination.totalCount}
          />
        ) : null}

        <ul className={styles.oversikt}>
          {tiltaksgjennomforinger.length === 0 && (
            <>
              <Alert variant="info">
                Vi fant ingen tiltaksgjennomføringer som du har lagt til i din
                liste
                <p>
                  Du kan legge tiltaksgjennomføringer til i denne oversikten ved
                  å trykke deg inn på en spesifikk gjennomføring og velge
                  &ldquo;Legg til i min liste&rdquo;.
                </p>
              </Alert>
            </>
          )}
          {tiltaksgjennomforinger
            .sort((a, b) => a.navn.localeCompare(b.navn))
            .map((tiltaksgjennomforing) => (
              <Tiltaksgjennomforingrad
                key={tiltaksgjennomforing.id}
                tiltaksgjennomforing={tiltaksgjennomforing}
              />
            ))}
        </ul>
        <div className={styles.under_oversikt}>
          {tiltaksgjennomforinger.length > 0 ? (
            <>
              <PagineringsOversikt
                page={page}
                antall={tiltaksgjennomforinger.length}
                maksAntall={data.pagination.totalCount}
              />
              <Pagination
                size="small"
                data-testid="paginering"
                page={page}
                onPageChange={setPage}
                count={Math.ceil(
                  (data.pagination?.totalCount ?? PAGE_SIZE) / PAGE_SIZE
                )}
                data-version="v1"
              />
            </>
          ) : null}
        </div>
      </>
    </>
  );
}
