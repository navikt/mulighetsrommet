import { Alert, BodyShort, Heading, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { paginationAtom } from "../../api/atoms";
import { useTiltaksgjennomforingerByInnloggetAnsatt } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByInnloggetAnsatt";
import { Laster } from "../../components/Laster";
import { PagineringsOversikt } from "../../components/paginering/PagineringOversikt";
import { SokEtterTiltaksgjennomforing } from "../../components/sok/SokEtterTiltaksgjennomforing";
import { TiltaksgjennomforingRad } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingRad";
import { PAGE_SIZE } from "../../constants";
import styles from "../Oversikt.module.scss";
import { ListeheaderTiltaksgjennomforinger } from "../../components/listeelementer/Listeheader";

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
        Det oppsto en feil ved henting av dine tiltaksgjennomføringer. Prøv
        igjen senere.
      </Alert>
    );
  }

  if (!data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne dine tiltaksgjennomføringer.
      </Alert>
    );
  }

  const tiltaksgjennomforinger = data.data;
  return (
    <>
      <Heading size="large">Oversikt over mine tiltaksgjennomføringer</Heading>
      <BodyShort className={styles.body} size="small">
        Her finner du gjennomføringer du har lagt til i din liste
      </BodyShort>
      <SokEtterTiltaksgjennomforing />

      {tiltaksgjennomforinger.length === 0 ? (
        <Alert variant="info">
          <Heading size={"xsmall"}>
            Vi fant ingen tiltaksgjennomføringer som du har lagt til i din liste
          </Heading>
          Du kan legge tiltaksgjennomføringer til i denne oversikten ved å
          trykke deg inn på en spesifikk gjennomføring og velge &ldquo;Legg til
          i min liste&rdquo;.
        </Alert>
      ) : (
        <>
          {tiltaksgjennomforinger.length > 0 ? (
            <PagineringsOversikt
              page={page}
              antall={tiltaksgjennomforinger.length}
              maksAntall={data.pagination.totalCount}
            />
          ) : null}

          <ul className={styles.oversikt}>
            <ListeheaderTiltaksgjennomforinger />
            {tiltaksgjennomforinger
              .sort((a, b) => a.navn.localeCompare(b.navn))
              .map((tiltaksgjennomforing) => (
                <TiltaksgjennomforingRad
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
      )}
    </>
  );
}
