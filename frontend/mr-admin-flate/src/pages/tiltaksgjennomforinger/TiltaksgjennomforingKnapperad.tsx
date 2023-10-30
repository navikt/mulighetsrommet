import { Button, Switch } from "@navikt/ds-react";
import { Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useMutateTilgjengeligForVeileder } from "../../api/tiltaksgjennomforing/useMutateTilgjengeligForVeileder";
import { useTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforing";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import styles from "../DetaljerInfo.module.scss";

interface Props {
  handleSlett: () => void;
  style?: React.CSSProperties;
}

export function TiltaksgjennomforingKnapperad({ handleSlett, style }: Props) {
  const { mutate } = useMutateTilgjengeligForVeileder();
  const { data, refetch } = useTiltaksgjennomforing();

  const { data: slettGjennomforingIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_TILTAKSGJENNOMFORING,
  );

  function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    if (data?.id) {
      mutate(
        { id: data.id, tilgjengeligForVeileder: e.currentTarget.checked },
        { onSettled: () => refetch() },
      );
    }
  }

  return (
    <div style={style} className={styles.knapperad}>
      <Switch checked={!!data?.tilgjengeligForVeileder} onClick={handleClick}>
        Tilgjengelig for veileder
      </Switch>
      {slettGjennomforingIsEnabled ? (
        <Button
          style={{
            marginRight: "1rem",
          }}
          size="small"
          variant="tertiary-neutral"
          onClick={handleSlett}
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
