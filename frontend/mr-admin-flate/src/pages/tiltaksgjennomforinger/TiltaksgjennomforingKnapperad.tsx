import { Switch } from "@navikt/ds-react";
import { useMutateTilgjengeligForVeileder } from "../../api/tiltaksgjennomforing/useMutateTilgjengeligForVeileder";
import { useTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforing";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import styles from "../DetaljerInfo.module.scss";

interface Props {
  style?: React.CSSProperties;
}

export function TiltaksgjennomforingKnapperad({ style }: Props) {
  const { mutate } = useMutateTilgjengeligForVeileder();
  const { data, refetch } = useTiltaksgjennomforing();

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

      <Lenkeknapp size="small" to={`skjema`} variant="primary">
        Rediger
      </Lenkeknapp>
    </div>
  );
}
