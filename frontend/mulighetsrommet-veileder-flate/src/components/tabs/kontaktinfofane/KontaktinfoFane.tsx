import { Alert } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import FaneTiltaksinformasjon from "../FaneTiltaksinformasjon";
import ArrangorInfo from "./ArrangorInfo";
import styles from "./Kontaktinfo.module.scss";
import NavKontaktpersonInfo from "./NavKontaktpersonInfo";

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
      <div className={styles.grid_container}>
        <ArrangorInfo
          arrangor={tiltaksgjennomforing.arrangor}
          faneinnhold={tiltaksgjennomforing.faneinnhold?.kontaktinfo}
        />
        <NavKontaktpersonInfo kontaktinfo={tiltaksgjennomforing.kontaktinfo} />
      </div>
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
