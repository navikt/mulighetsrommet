import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import React, { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { useSetPublisert } from "@/api/gjennomforing/useSetPublisert";
import {
  GjennomforingDetaljerDto,
  GjennomforingDto,
  GjennomforingHandling,
  GjennomforingVeilederinfoDto,
  NavAnsattDto,
} from "@tiltaksadministrasjon/api-client";
import { DeepPartial } from "react-hook-form";
import { AvbrytGjennomforingModal } from "@/components/gjennomforing/AvbrytGjennomforingModal";

interface Props {
  ansatt: NavAnsattDto;
  gjennomforing: GjennomforingDto;
  veilederinfo: GjennomforingVeilederinfoDto | null;
  handlinger: GjennomforingHandling[];
}

export function GjennomforingKnapperad({ ansatt, gjennomforing, veilederinfo, handlinger }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);

  const { mutate: setPublisert } = useSetPublisert(gjennomforing.id);

  async function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  function dupliserGjennomforing() {
    const duplisert: DeepPartial<GjennomforingDetaljerDto> = {
      gjennomforing: {
        avtaleId: gjennomforing.avtaleId,
      },
      veilederinfo: {
        beskrivelse: veilederinfo?.beskrivelse,
        faneinnhold: veilederinfo?.faneinnhold,
      },
    };

    setGjennomforingDetaljerTab("detaljer");
    navigate(`/avtaler/${gjennomforing.avtaleId}/gjennomforinger/skjema`, {
      state: { dupliserGjennomforing: duplisert },
    });
  }

  return (
    <KnapperadContainer>
      {veilederinfo && handlinger.includes(GjennomforingHandling.PUBLISER) && (
        <Switch name="publiser" checked={veilederinfo.publisert} onClick={togglePublisert}>
          Publiser
        </Switch>
      )}
      <EndringshistorikkPopover>
        <GjennomforingEndringshistorikk id={gjennomforing.id} />
      </EndringshistorikkPopover>
      <Dropdown>
        <Button size="small" variant="secondary" as={Dropdown.Toggle}>
          Handlinger
        </Button>
        <Dropdown.Menu>
          <Dropdown.Menu.GroupedList>
            {handlinger.includes(GjennomforingHandling.REDIGER) && (
              <Dropdown.Menu.GroupedList.Item
                onClick={() => {
                  if (
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
            )}
            {veilederinfo &&
              handlinger.includes(GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => apentForPameldingModalRef.current?.showModal()}
                >
                  {veilederinfo.apentForPamelding ? "Steng for påmelding" : "Åpne for påmelding"}
                </Dropdown.Menu.GroupedList.Item>
              )}
            {handlinger.includes(GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR) && (
              <Dropdown.Menu.GroupedList.Item
                onClick={() => registrerStengtModalRef.current?.showModal()}
              >
                Registrer stengt hos arrangør
              </Dropdown.Menu.GroupedList.Item>
            )}
            {handlinger.includes(GjennomforingHandling.AVBRYT) && (
              <Dropdown.Menu.GroupedList.Item onClick={() => setAvbrytModalOpen(true)}>
                Avbryt gjennomføring
              </Dropdown.Menu.GroupedList.Item>
            )}
          </Dropdown.Menu.GroupedList>
          {handlinger.includes(GjennomforingHandling.DUPLISER) && (
            <>
              <Dropdown.Menu.Divider />
              <Dropdown.Menu.List>
                <Dropdown.Menu.List.Item onClick={dupliserGjennomforing}>
                  <LayersPlusIcon fontSize="1.5rem" aria-label="Ikon for duplisering av dokument" />
                  Dupliser
                </Dropdown.Menu.List.Item>
              </Dropdown.Menu.List>
            </>
          )}
        </Dropdown.Menu>
      </Dropdown>
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
        gjennomforingId={gjennomforing.id}
        stengt={gjennomforing.stengt}
      />
      <SetApentForPameldingModal
        modalRef={apentForPameldingModalRef}
        gjennomforingId={gjennomforing.id}
      />
      <AvbrytGjennomforingModal
        open={avbrytModalOpen}
        setOpen={setAvbrytModalOpen}
        gjennomforingId={gjennomforing.id}
      />
    </KnapperadContainer>
  );
}

function GjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useGjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
