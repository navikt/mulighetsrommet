import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import FaneTiltaksinformasjon from "../FaneTiltaksinformasjon";
import ArrangorInfo from "./ArrangorInfo";
import styles from "./Kontaktinfo.module.scss";
import NavKontaktpersonInfo from "./NavKontaktpersonInfo";
import { Alert, BodyLong } from "@navikt/ds-react";
import { RedaksjoneltInnhold } from "../../RedaksjoneltInnhold";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const KontaktinfoFane = ({ tiltaksgjennomforing }: Props) => {
  return (
    <FaneTiltaksinformasjon
      harInnhold={!!tiltaksgjennomforing}
      className={styles.kontaktinfo_container}
    >
      {tiltaksgjennomforing.faneinnhold?.kontaktinfoInfoboks && (
        <Alert variant="info" className={styles.preWrap}>
          {tiltaksgjennomforing.faneinnhold.kontaktinfoInfoboks}
        </Alert>
      )}
      {tiltaksgjennomforing.faneinnhold?.kontaktinfo && (
        <BodyLong as="div" textColor="subtle" size="small">
          <RedaksjoneltInnhold value={tiltaksgjennomforing.faneinnhold?.kontaktinfo} />
        </BodyLong>
      )}
      <div className={styles.grid_container}>
        <ArrangorInfo arrangor={tiltaksgjennomforing.arrangor} />
        <NavKontaktpersonInfo kontaktinfo={tiltaksgjennomforing.kontaktinfo} />
      </div>
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
