import { Alert } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client";
import FaneTiltaksinformasjon from "../FaneTiltaksinformasjon";
import ArrangorInfo from "./ArrangorInfo";
import styles from "./Kontaktinfo.module.scss";
import NavKontaktpersonInfo from "./NavKontaktpersonInfo";
import { isTiltakGruppe } from "@/api/queries/useTiltaksgjennomforingById";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltak;
}

const KontaktinfoFane = ({ tiltaksgjennomforing }: Props) => {
  return (
    <FaneTiltaksinformasjon
      harInnhold={!!tiltaksgjennomforing}
      className={styles.kontaktinfo_container}
    >
      {tiltaksgjennomforing.faneinnhold?.kontaktinfoInfoboks && (
        <Alert variant="info" style={{ whiteSpace: "pre-wrap" }}>
          {tiltaksgjennomforing.faneinnhold.kontaktinfoInfoboks}
        </Alert>
      )}
      <div className={styles.grid_container}>
        {isTiltakGruppe(tiltaksgjennomforing) && (
          <ArrangorInfo
            arrangor={tiltaksgjennomforing.arrangor}
            faneinnhold={tiltaksgjennomforing.faneinnhold?.kontaktinfo}
          />
        )}
        <NavKontaktpersonInfo kontaktinfo={tiltaksgjennomforing.kontaktinfo} />
      </div>
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
