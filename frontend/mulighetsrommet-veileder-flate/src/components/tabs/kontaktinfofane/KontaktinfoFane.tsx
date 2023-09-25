import { Alert } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { erPreview } from "../../../utils/Utils";
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
      {erPreview ? (
        <Alert variant="info">Ved forhåndsvisning vises ikke kontaktinformasjon</Alert>
      ) : (
        <>
          <ArrangorInfo data={tiltaksgjennomforing} />
          <NavKontaktpersonInfo data={tiltaksgjennomforing} />
        </>
      )}
    </FaneTiltaksinformasjon>
  );
};

export default KontaktinfoFane;
