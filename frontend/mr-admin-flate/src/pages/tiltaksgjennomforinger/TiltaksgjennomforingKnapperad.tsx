import { Button } from "@navikt/ds-react";
import { Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import styles from "../DetaljerInfo.module.scss";

interface Props {
  handleSlett: () => void;
  style?: React.CSSProperties;
}

export function TiltaksgjennomforingKnapperad({ handleSlett, style }: Props) {
  const { data: slettGjennomforingIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_TILTAKSGJENNOMFORING,
  );

  return (
    <div style={style}>
      {slettGjennomforingIsEnabled ? (
        <Button
          style={{
            marginRight: "1rem",
          }}
          size="small"
          variant="tertiary-neutral"
          onClick={handleSlett}
          data-testid="slett-gjennomforing"
          className={styles.slett_knapp}
        >
          Feilregistrering
        </Button>
      ) : null}

      <Lenkeknapp size="small" to={`skjema`} variant="primary">
        Rediger
      </Lenkeknapp>
    </div>
  );
}
