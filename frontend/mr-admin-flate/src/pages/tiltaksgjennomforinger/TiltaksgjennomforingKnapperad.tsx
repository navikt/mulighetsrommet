import { useMutatePublisert } from "@/api/tiltaksgjennomforing/useMutatePublisert";
import { useTiltaksgjennomforingEndringshistorikk } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { AvbrytGjennomforingModal } from "@/components/modal/AvbrytGjennomforingModal";
import { SetApentForPameldingModal } from "@/components/tiltaksgjennomforinger/SetApentForPameldingModal";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import { NavAnsatt, TiltaksgjennomforingDto } from "@mr/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import React, { useRef } from "react";
import { useNavigate, useRevalidator } from "react-router-dom";

interface Props {
  ansatt: NavAnsatt;
  tiltaksgjennomforing: TiltaksgjennomforingDto;
}

export function TiltaksgjennomforingKnapperad({ ansatt, tiltaksgjennomforing }: Props) {
  const navigate = useNavigate();
  const { mutate } = useMutatePublisert();
  const revalidate = useRevalidator();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);

  function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    mutate(
      { id: tiltaksgjennomforing.id, publisert: e.currentTarget.checked },
      {
        onSuccess: revalidate.revalidate,
      },
    );
  }

  return (
    <KnapperadContainer>
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
        <Dropdown>
          <Button size="small" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            <Dropdown.Menu.GroupedList>
              <Dropdown.Menu.GroupedList.Item
                onClick={() => {
                  if (
                    tiltaksgjennomforing.administratorer &&
                    tiltaksgjennomforing.administratorer.length > 0 &&
                    !tiltaksgjennomforing.administratorer
                      .map((a) => a.navIdent)
                      .includes(ansatt.navIdent)
                  ) {
                    advarselModal.current?.showModal();
                  } else {
                    navigate("skjema");
                  }
                }}
              >
                Rediger gjennomføring
              </Dropdown.Menu.GroupedList.Item>
              {gjennomforingIsAktiv(tiltaksgjennomforing.status.status) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => apentForPameldingModalRef.current?.showModal()}
                >
                  {tiltaksgjennomforing.apentForPamelding
                    ? "Steng for påmelding"
                    : "Åpne for påmelding"}
                </Dropdown.Menu.GroupedList.Item>
              )}

              {gjennomforingIsAktiv(tiltaksgjennomforing.status.status) && (
                <Dropdown.Menu.GroupedList.Item onClick={() => avbrytModalRef.current?.showModal()}>
                  Avbryt gjennomføring
                </Dropdown.Menu.GroupedList.Item>
              )}
            </Dropdown.Menu.GroupedList>
          </Dropdown.Menu>
        </Dropdown>
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
      <SetApentForPameldingModal
        modalRef={apentForPameldingModalRef}
        gjennomforing={tiltaksgjennomforing}
      />
      <AvbrytGjennomforingModal
        modalRef={avbrytModalRef}
        tiltaksgjennomforing={tiltaksgjennomforing}
      />
    </KnapperadContainer>
  );
}

function TiltaksgjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useTiltaksgjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
