import { Tiltaksgjennomforingrad } from "./Tiltaksgjennomforing";
import { Alert, Heading, Loader, Pagination } from "@navikt/ds-react";
import styles from "./TiltaksgjennomforingslisteForTiltakstyper.module.scss";
import tiltaksgjennomforingsStyles from "./Tiltaksgjennomforingeroversikt.module.scss";
import { useTiltaksgjennomforingerByTiltakskode } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingerByTiltakskode";
import { PAGE_SIZE } from "../../constants";
import { useAtom } from "jotai";
import { paginationAtomTiltaksgjennomforingMedTiltakstype } from "../../api/atoms";

interface Props {
  tiltakstypeKode: string;
}

export function TiltaksgjennomforingslisteForTiltakstyper({
  tiltakstypeKode,
}: Props) {
  const [page, setPage] = useAtom(
    paginationAtomTiltaksgjennomforingMedTiltakstype
  );
  const { data: tiltaksgjennomforinger, isLoading } =
    useTiltaksgjennomforingerByTiltakskode(tiltakstypeKode);
  if (isLoading) {
    return <Loader size="xlarge" />;
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
            <Tiltaksgjennomforingrad
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
