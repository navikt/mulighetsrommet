import { useMutatePublisert } from "@/api/gjennomforing/useMutatePublisert";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { AvbrytGjennomforingModal } from "@/components/modal/AvbrytGjennomforingModal";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import { NavAnsatt, TiltaksgjennomforingDto } from "@mr/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import React, { useRef } from "react";
import { useNavigate, useRevalidator } from "react-router";

interface Props {
  ansatt: NavAnsatt;
  gjennomforing: TiltaksgjennomforingDto;
}

export function GjennomforingKnapperad({ ansatt, gjennomforing }: Props) {
  const navigate = useNavigate();
  const { mutate } = useMutatePublisert();
  const revalidate = useRevalidator();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);

  function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    mutate(
      { id: gjennomforing.id, publisert: e.currentTarget.checked },
      {
        onSuccess: revalidate.revalidate,
      },
    );
  }

  return (
    <KnapperadContainer>
      <HarSkrivetilgang
        ressurs="Tiltaksgjennomføring"
        condition={gjennomforingIsAktiv(gjennomforing.status.status)}
      >
        <Switch checked={gjennomforing.publisert} onClick={handleClick}>
          Publiser
        </Switch>
      </HarSkrivetilgang>

      <EndringshistorikkPopover>
        <TiltaksgjennomforingEndringshistorikk id={gjennomforing.id} />
      </EndringshistorikkPopover>

      <HarSkrivetilgang
        ressurs="Tiltaksgjennomføring"
        condition={gjennomforingIsAktiv(gjennomforing.status.status)}
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
                    gjennomforing.administratorer &&
                    gjennomforing.administratorer.length > 0 &&
                    !gjennomforing.administratorer.map((a) => a.navIdent).includes(ansatt.navIdent)
                  ) {
                    advarselModal.current?.showModal();
                  } else {
                    navigate("skjema");
                  }
                }}
              >
                Rediger gjennomføring
              </Dropdown.Menu.GroupedList.Item>
              {gjennomforingIsAktiv(gjennomforing.status.status) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => apentForPameldingModalRef.current?.showModal()}
                >
                  {gjennomforing.apentForPamelding ? "Steng for påmelding" : "Åpne for påmelding"}
                </Dropdown.Menu.GroupedList.Item>
              )}

              {gjennomforingIsAktiv(gjennomforing.status.status) && (
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
        gjennomforing={gjennomforing}
      />
      <AvbrytGjennomforingModal modalRef={avbrytModalRef} gjennomforing={gjennomforing} />
    </KnapperadContainer>
  );
}

function TiltaksgjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useGjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
