import { Alert, Heading, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { paginationAtom } from "../../api/atoms";
import { useTiltaksgjennomforingerByEnhet } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByEnhet";
import { Laster } from "../../components/Laster";
import { PagineringsOversikt } from "../../components/paginering/PagineringOversikt";
import { SokEtterTiltaksgjennomforing } from "../../components/sok/SokEtterTiltaksgjennomforing";
import { TiltaksgjennomforingRad } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingRad";
import { PAGE_SIZE } from "../../constants";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";

export function EnhetsoversiktPage() {
  const { data: ansattData } = useHentAnsatt();
  const { data, isFetching, isError } = useTiltaksgjennomforingerByEnhet(
    ansattData?.hovedenhet
  );
  const [page, setPage] = useAtom(paginationAtom);

  if (isFetching || !ansattData) {
    return <Laster />;
  }

  if (isError) {
    return (
      <Alert variant="error">
        Det oppsto en feil ved henting av tiltaksgjennomføringer for din enhet.
        Prøv igjen senere.
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  if (!data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennomføringer for din enhet
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  const tiltaksgjennomforinger = data.data;
  return (
    <>
      <Heading size="large">
        Oversikt over tiltaksgjennomføringer for enhet:{" "}
        {ansattData?.hovedenhetNavn}
      </Heading>
      <BodyShort className={styles.body} size="small">
        Her finner du alle gjennomføringer for din enhet
      </BodyShort>
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
            <Alert variant="info">
              Vi fant ingen tiltaksgjennomføringer for din enhet
            </Alert>
          )}
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
      {/**
       * TODO Implementere skjema for opprettelse av tiltakstype
       */}
    </>
  );
}
