import React, { useRef } from "react";
import { Button, Switch } from "@navikt/ds-react";
import { useMutatePublisert } from "../../api/tiltaksgjennomforing/useMutatePublisert";
import styles from "../DetaljerInfo.module.scss";
import {
  NavAnsatt,
  Tiltaksgjennomforing,
  TiltaksgjennomforingStatus,
} from "mulighetsrommet-api-client";
import { useTiltaksgjennomforingEndringshistorikk } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingEndringshistorikk";
import { EndringshistorikkPopover } from "../../components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "../../components/endringshistorikk/ViewEndringshistorikk";
import { RedigeringsAdvarselModal } from "../../components/modal/RedigeringsAdvarselModal";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "../../components/authActions/HarSkrivetilgang";

interface Props {
  bruker: NavAnsatt;
  tiltaksgjennomforing: Tiltaksgjennomforing;
}

export function TiltaksgjennomforingKnapperad({ bruker, tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const { mutate } = useMutatePublisert();
  const advarselModal = useRef<HTMLDialogElement>(null);

  function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    mutate({ id: tiltaksgjennomforing.id, publisert: e.currentTarget.checked });
  }

  const gjennomforingIsActive = [
    TiltaksgjennomforingStatus.PLANLAGT,
    TiltaksgjennomforingStatus.GJENNOMFORES,
  ].includes(tiltaksgjennomforing.status);

  return (
    <div className={styles.knapperad}>
      <HarSkrivetilgang ressurs="Tiltaksgjennomføring" condition={gjennomforingIsActive}>
        <Switch checked={tiltaksgjennomforing.publisert} onClick={handleClick}>
          Publiser
        </Switch>
      </HarSkrivetilgang>

      <EndringshistorikkPopover>
        <TiltaksgjennomforingEndringshistorikk id={tiltaksgjennomforing.id} />
      </EndringshistorikkPopover>

      <HarSkrivetilgang ressurs="Tiltaksgjennomføring" condition={gjennomforingIsActive}>
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
