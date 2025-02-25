import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { AvbrytGjennomforingModal } from "@/components/modal/AvbrytGjennomforingModal";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import { GjennomforingDto, NavAnsatt, Toggles } from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { Alert, BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import React, { useRef } from "react";
import { useFetcher, useNavigate } from "react-router";
interface Props {
  ansatt: NavAnsatt;
  gjennomforing: GjennomforingDto;
}

export function GjennomforingKnapperad({ ansatt, gjennomforing }: Props) {
  const navigate = useNavigate();
  const fetcher = useFetcher();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  // Add error state handling
  const publiseringErrored = fetcher.data?.error;

  async function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    fetcher.submit(
      { id: gjennomforing.id, publisert: e.currentTarget.checked },
      {
        action: `/gjennomforinger/${gjennomforing.id}`,
        method: "post",
      },
    );
  }

  let gjennomforingPublisert = gjennomforing.publisert;
  if (!publiseringErrored && fetcher.formData) {
    gjennomforingPublisert = fetcher.formData.get("publisert") === "true" ? true : false;
  }

  return (
    <KnapperadContainer>
      <HarSkrivetilgang
        ressurs="Gjennomføring"
        condition={gjennomforingIsAktiv(gjennomforing.status.status)}
      >
        <div>
          <Switch name="publiser" checked={gjennomforingPublisert} onClick={handleClick}>
            Publiser
          </Switch>
          {publiseringErrored && (
            <Alert variant="warning" inline>
              Det oppstod en feil ved publisering. Prøv igjen senere.
            </Alert>
          )}
        </div>
      </HarSkrivetilgang>

      <EndringshistorikkPopover>
        <GjennomforingEndringshistorikk id={gjennomforing.id} />
      </EndringshistorikkPopover>

      <HarSkrivetilgang
        ressurs="Gjennomføring"
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
              {enableOkonomi && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => registrerStengtModalRef.current?.showModal()}
                >
                  Registrer stengt hos arrangør
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
      <RegistrerStengtHosArrangorModal
        modalRef={registrerStengtModalRef}
        gjennomforing={gjennomforing}
      />
      <SetApentForPameldingModal
        modalRef={apentForPameldingModalRef}
        gjennomforing={gjennomforing}
      />
      <AvbrytGjennomforingModal modalRef={avbrytModalRef} gjennomforing={gjennomforing} />
    </KnapperadContainer>
  );
}

function GjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useGjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
