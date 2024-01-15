import React, { useRef } from "react";
import { Button, Switch } from "@navikt/ds-react";
import { useMutateTilgjengeligForVeileder } from "../../api/tiltaksgjennomforing/useMutateTilgjengeligForVeileder";
import styles from "../DetaljerInfo.module.scss";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useTiltaksgjennomforingEndringshistorikk } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingEndringshistorikk";
import { EndringshistorikkPopover } from "../../components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "../../components/endringshistorikk/ViewEndringshistorikk";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { Laster } from "../../components/laster/Laster";
import { RedigeringsAdvarselModal } from "../../components/modal/RedigeringsAdvarselModal";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "../../components/authActions/HarSkrivetilgang";

interface Props {
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function TiltaksgjennomforingKnapperad({ tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const { mutate } = useMutateTilgjengeligForVeileder();
  const { data: bruker } = useHentAnsatt();
  const advarselModal = useRef<HTMLDialogElement>(null);

  function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    mutate({ id: tiltaksgjennomforing.id, tilgjengeligForVeileder: e.currentTarget.checked });
  }
  if (!bruker) {
    return <Laster />;
  }

  return (
    <div className={styles.knapperad}>
      <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
        <Switch checked={tiltaksgjennomforing.tilgjengeligForVeileder} onClick={handleClick}>
          Tilgjengelig for veileder
        </Switch>
      </HarSkrivetilgang>

      <EndringshistorikkPopover>
        <TiltaksgjennomforingEndringshistorikk id={tiltaksgjennomforing.id} />
      </EndringshistorikkPopover>

      <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
        <Button
          size="small"
          variant="primary"
          onClick={() => {
            if (
              tiltaksgjennomforing.administratorer &&
              tiltaksgjennomforing.administratorer.length > 0 &&
              !tiltaksgjennomforing.administratorer.map((a) => a.navIdent).includes(bruker.navIdent)
            ) {
              advarselModal.current?.showModal();
            } else {
              navigate("skjema");
            }
          }}
        >
          Rediger
        </Button>
      </HarSkrivetilgang>
      <RedigeringsAdvarselModal
        ressursNavn="tiltaksgjennomføringen"
        modalRef={advarselModal}
        onRediger={() => navigate("skjema")}
      />
    </div>
  );
}

function TiltaksgjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useTiltaksgjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
