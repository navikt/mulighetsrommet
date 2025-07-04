import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { AvbrytGjennomforingModal } from "@/components/modal/AvbrytGjennomforingModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
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
import { BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import React, { useRef } from "react";
import { useNavigate } from "react-router";
import { useSetPublisert } from "@/api/gjennomforing/useSetPublisert";

interface Props {
  ansatt: NavAnsatt;
  avtale?: AvtaleDto;
  gjennomforing: GjennomforingDto;
}

export function GjennomforingKnapperad({ ansatt, avtale, gjennomforing }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);

  const { mutate: setPublisert } = useSetPublisert(gjennomforing.id);

  async function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
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
        condition={gjennomforing.status.type === GjennomforingStatus.GJENNOMFORES}
      >
        <Switch name="publiser" checked={gjennomforing.publisert} onClick={togglePublisert}>
          Publiser
        </Switch>
      </HarSkrivetilgang>

      <EndringshistorikkPopover>
        <GjennomforingEndringshistorikk id={gjennomforing.id} />
      </EndringshistorikkPopover>
      <HarSkrivetilgang ressurs="Gjennomføring">
        <Dropdown>
          <Button size="small" variant="secondary" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            {gjennomforing.status.type === GjennomforingStatus.GJENNOMFORES && (
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
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => apentForPameldingModalRef.current?.showModal()}
                  >
                    {gjennomforing.apentForPamelding ? "Steng for påmelding" : "Åpne for påmelding"}
                  </Dropdown.Menu.GroupedList.Item>
                  {avtale?.prismodell === Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK && (
                    <Dropdown.Menu.GroupedList.Item
                      onClick={() => registrerStengtModalRef.current?.showModal()}
                    >
                      Registrer stengt hos arrangør
                    </Dropdown.Menu.GroupedList.Item>
                  )}
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => avbrytModalRef.current?.showModal()}
                  >
                    Avbryt gjennomføring
                  </Dropdown.Menu.GroupedList.Item>
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
