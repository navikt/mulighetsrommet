import React from "react";
import { Switch } from "@navikt/ds-react";
import { useMutateTilgjengeligForVeileder } from "../../api/tiltaksgjennomforing/useMutateTilgjengeligForVeileder";
import { Lenkeknapp } from "../../components/lenkeknapp/Lenkeknapp";
import styles from "../DetaljerInfo.module.scss";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useTiltaksgjennomforingEndringshistorikk } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingEndringshistorikk";
import { EndringshistorikkPopover } from "../../components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "../../components/endringshistorikk/ViewEndringshistorikk";

interface Props {
  style?: React.CSSProperties;
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function TiltaksgjennomforingKnapperad({ style, tiltaksgjennomforing }: Props) {
  const { mutate } = useMutateTilgjengeligForVeileder();

  function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    mutate({ id: tiltaksgjennomforing.id, tilgjengeligForVeileder: e.currentTarget.checked });
  }

  return (
    <div style={style} className={styles.knapperad}>
      <Switch checked={tiltaksgjennomforing.tilgjengeligForVeileder} onClick={handleClick}>
        Tilgjengelig for veileder
      </Switch>

      <EndringshistorikkPopover>
        <TiltaksgjennomforingEndringshistorikk id={tiltaksgjennomforing.id} />
      </EndringshistorikkPopover>

      <Lenkeknapp size="small" to={`skjema`} variant="primary">
        Rediger
      </Lenkeknapp>
    </div>
  );
}

function TiltaksgjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useTiltaksgjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
