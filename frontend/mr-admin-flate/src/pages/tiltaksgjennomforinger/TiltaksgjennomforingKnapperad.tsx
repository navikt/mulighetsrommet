import styles from "../DetaljerInfo.module.scss";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";

export function TiltaksgjennomforingKnapperad() {
  const { data: redigerGjennomforingIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_REDIGER_TILTAKSGJENNOMFORING,
  );

  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  return (
    <div className={styles.knapperad}>
      {redigerGjennomforingIsEnabled ? (
        <Lenkeknapp
          to={`/tiltaksgjennomforinger/skjema?tiltaksgjennomforingId=${tiltaksgjennomforingId}`}
          lenketekst="Rediger tiltaksgjennomføring"
          variant="primary"
        />
      ) : null}
    </div>
  );
}
