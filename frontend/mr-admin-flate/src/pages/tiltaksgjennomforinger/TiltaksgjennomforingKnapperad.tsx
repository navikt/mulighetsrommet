import React, { useRef } from "react";
import { BodyShort, Button, Switch } from "@navikt/ds-react";
import { useMutatePublisert } from "@/api/tiltaksgjennomforing/useMutatePublisert";
import styles from "../DetaljerInfo.module.scss";
import { NavAnsatt, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useTiltaksgjennomforingEndringshistorikk } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingEndringshistorikk";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { VarselModal } from "@/components/modal/VarselModal";
import { gjennomforingIsAktiv } from "mulighetsrommet-frontend-common/utils/utils";

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

  return (
    <div className={styles.knapperad}>
      <HarSkrivetilgang
        ressurs="Tiltaksgjennomføring"
        condition={gjennomforingIsAktiv(tiltaksgjennomforing.status.status)}
      >
        <Switch checked={tiltaksgjennomforing.publisert} onClick={handleClick}>
          Publiser
        </Switch>
      </HarSkrivetilgang>

      <EndringshistorikkPopover>
        <TiltaksgjennomforingEndringshistorikk id={tiltaksgjennomforing.id} />
      </EndringshistorikkPopover>

      <HarSkrivetilgang
        ressurs="Tiltaksgjennomføring"
        condition={gjennomforingIsAktiv(tiltaksgjennomforing.status.status)}
      >
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
      <VarselModal
        modalRef={advarselModal}
        handleClose={() => advarselModal.current?.close()}
        headingIconType="info"
        headingText="Du er ikke eier av denne tiltaksgjennomføringen"
        body={<BodyShort>Vil du fortsette til redigeringen?</BodyShort>}
        secondaryButton
        primaryButton={
          <Button variant="primary" onClick={() => navigate("skjema")}>
            Ja, jeg vil redigere
          </Button>
        }
      />
    </div>
  );
}

function TiltaksgjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useTiltaksgjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
