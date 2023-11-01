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
      <ArrangorInfo data={tiltaksgjennomforing} />
      <NavKontaktpersonInfo data={tiltaksgjennomforing} />
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
