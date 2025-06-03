import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { AvbrytGjennomforingModal } from "@/components/modal/AvbrytGjennomforingModal";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import {
  AvtaleDto,
  GjennomforingDto,
  GjennomforingStatus,
  NavAnsatt,
  Opphav,
  Prismodell,
} from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import React, { useRef } from "react";
import { useFetcher, useNavigate } from "react-router";

interface Props {
  ansatt: NavAnsatt;
  avtale?: AvtaleDto;
  gjennomforing: GjennomforingDto;
}

export function GjennomforingKnapperad({ ansatt, avtale, gjennomforing }: Props) {
  const navigate = useNavigate();
  const fetcher = useFetcher();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);

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
    gjennomforingPublisert = fetcher.formData.get("publisert") === "true";
  }

  function dupliserGjennomforing() {
    setGjennomforingDetaljerTab("detaljer");
    navigate(`/avtaler/${gjennomforing.avtaleId}/gjennomforinger/skjema`, {
      state: {
        dupliserGjennomforing: {
          opphav: Opphav.TILTAKSADMINISTRASJON,
          avtaleId: gjennomforing.avtaleId,
          beskrivelse: gjennomforing.beskrivelse,
          faneinnhold: gjennomforing.faneinnhold,
        },
      },
    });
  }

  return (
    <KnapperadContainer>
      <HarSkrivetilgang
        ressurs="Gjennomføring"
        condition={gjennomforing.status.status === GjennomforingStatus.GJENNOMFORES}
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
      <HarSkrivetilgang ressurs="Gjennomføring">
        <Dropdown>
          <Button size="small" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            {gjennomforing.status.status === GjennomforingStatus.GJENNOMFORES && (
              <>
                <Dropdown.Menu.GroupedList>
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => {
                      if (
                        gjennomforing.administratorer &&
                        gjennomforing.administratorer.length > 0 &&
                        !gjennomforing.administratorer
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
                  {gjennomforing.status.status === GjennomforingStatus.GJENNOMFORES && (
                    <Dropdown.Menu.GroupedList.Item
                      onClick={() => apentForPameldingModalRef.current?.showModal()}
                    >
                      {gjennomforing.apentForPamelding
                        ? "Steng for påmelding"
                        : "Åpne for påmelding"}
                    </Dropdown.Menu.GroupedList.Item>
                  )}
                  {avtale?.prismodell === Prismodell.FORHANDSGODKJENT && (
                    <Dropdown.Menu.GroupedList.Item
                      onClick={() => registrerStengtModalRef.current?.showModal()}
                    >
                      Registrer stengt hos arrangør
                    </Dropdown.Menu.GroupedList.Item>
                  )}
                  {gjennomforing.status.status === GjennomforingStatus.GJENNOMFORES && (
                    <Dropdown.Menu.GroupedList.Item
                      onClick={() => avbrytModalRef.current?.showModal()}
                    >
                      Avbryt gjennomføring
                    </Dropdown.Menu.GroupedList.Item>
                  )}
                </Dropdown.Menu.GroupedList>
                <Dropdown.Menu.Divider />
              </>
            )}
            <Dropdown.Menu.List>
              <Dropdown.Menu.List.Item onClick={dupliserGjennomforing}>
                <LayersPlusIcon fontSize="1.5rem" aria-label="Ikon for duplisering av dokument" />
                Dupliser
              </Dropdown.Menu.List.Item>
            </Dropdown.Menu.List>
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
