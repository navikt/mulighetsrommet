import { Button } from "@navikt/ds-react";
import { Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import styles from "../DetaljerInfo.module.scss";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import { useGetAdminTiltaksgjennomforingsIdFraUrl } from "../../hooks/useGetAdminTiltaksgjennomforingsIdFraUrl";

interface Props {
  handleSlett: () => void;
}

export function TiltaksgjennomforingKnapperad({ handleSlett }: Props) {
  const { data: slettGjennomforingIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_TILTAKSGJENNOMFORING,
  );
  const { data: redigerGjennomforingIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_REDIGER_TILTAKSGJENNOMFORING,
  );

  const tiltaksgjennomforingId = useGetAdminTiltaksgjennomforingsIdFraUrl();

  return (
    <div className={styles.knapperad}>
      {slettGjennomforingIsEnabled ? (
        <Button
          variant="tertiary-neutral"
          onClick={handleSlett}
          data-testid="slett-gjennomforing"
          className={styles.slett_knapp}
        >
          Feilregistrering
        </Button>
      ) : null}

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
