import { Alert, Heading, Pagination } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { paginationAtomTiltaksgjennomforingMedTiltakstype } from "../../api/atoms";
import { useTiltaksgjennomforingerByTiltakstypeId } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltakstypeId";
import { PAGE_SIZE } from "../../constants";
import { Laster } from "../Laster";
import { TiltaksgjennomforingRad } from "./TiltaksgjennomforingRad";
import tiltaksgjennomforingsStyles from "./Tiltaksgjennomforingeroversikt.module.scss";
import styles from "./TiltaksgjennomforingslisteForTiltakstyper.module.scss";
import { Tiltakstype } from "mulighetsrommet-api-client";

interface TiltaksgjennomforingslisteForTiltakstyperProps {
  tiltakstype: Tiltakstype;
}

export function TiltaksgjennomforingslisteForTiltakstyper({
  tiltakstype,
}: TiltaksgjennomforingslisteForTiltakstyperProps) {
  const [page, setPage] = useAtom(
    paginationAtomTiltaksgjennomforingMedTiltakstype
  );
  const { data: tiltaksgjennomforinger, isLoading } =
    useTiltaksgjennomforingerByTiltakstypeId(tiltakstype.id);

  if (isLoading) {
    return <Laster size="xlarge" />;
  }

  if (!tiltaksgjennomforinger) {
    return null;
  }

  const PagineringsOversikt = () => {
    return (
      <Heading level="2" size="xsmall" data-testid="antall-tiltak">
        Viser {(page - 1) * PAGE_SIZE + 1}-
        {tiltaksgjennomforinger.data.length + (page - 1) * PAGE_SIZE} av{" "}
        {tiltaksgjennomforinger.pagination?.totalCount} tiltak
      </Heading>
    );
  };

  return (
    <div className={styles.tiltaksgjennomforingsliste}>
      <Heading size="medium" level="2">
        Tiltaksgjennomføringer
      </Heading>
      {tiltaksgjennomforinger.data.length > 0 ? <PagineringsOversikt /> : null}

      <ul className={tiltaksgjennomforingsStyles.oversikt}>
        {tiltaksgjennomforinger.data.length === 0 && (
          <Alert variant="info">Ingen tilhørende tiltaksgjennomføringer</Alert>
        )}
        {tiltaksgjennomforinger.data
          .sort((a, b) => a.navn.localeCompare(b.navn))
          .map((tiltaksgjennomforing) => (
            <TiltaksgjennomforingRad
              fagansvarlig
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
        count={Math.ceil(
          (tiltaksgjennomforinger.pagination?.totalCount ?? PAGE_SIZE) /
            PAGE_SIZE
        )}
        data-version="v1"
      />
    </div>
  );
}
