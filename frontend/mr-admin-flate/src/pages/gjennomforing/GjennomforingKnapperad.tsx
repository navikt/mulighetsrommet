import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { GodkjennOkonomiModal } from "@/components/gjennomforing/GodkjennOkonomiModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { ExternalLinkIcon, LayersPlusIcon } from "@navikt/aksel-icons";
import { ActionMenu, BodyShort, Button, Switch } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import React, { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { useSetPublisert } from "@/api/gjennomforing/useSetPublisert";
import {
  GjennomforingAvtaleDto,
  GjennomforingDetaljerDto,
  GjennomforingDto,
  GjennomforingHandling,
  GjennomforingVeilederinfoDto,
  NavAnsattDto,
} from "@tiltaksadministrasjon/api-client";
import { DeepPartial } from "react-hook-form";
import { AvbrytGjennomforingModal } from "@/components/gjennomforing/AvbrytGjennomforingModal";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { previewArbeidsmarkedstiltakUrl } from "@/constants";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { AdministratorGuard } from "@/components/handlinger/AdministratorGuard";

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
  const [godkjennOkonomiModalOpen, setGodkjennOkonomiModalOpen] = useState<boolean>(false);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);

  const { mutate: setPublisert } = useSetPublisert(gjennomforing.id);

  async function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  function dupliserGjennomforing(gjennomforing: GjennomforingAvtaleDto) {
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
  const administratorer = isGruppetiltak(gjennomforing)
    ? gjennomforing.administratorer.map((a) => a.navIdent)
    : [];

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
      <Handlinger>
        {isGruppetiltak(gjennomforing) && handlinger.includes(GjennomforingHandling.REDIGER) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => navigate("skjema")}>
              Rediger gjennomføring
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {isGruppetiltak(gjennomforing) &&
          handlinger.includes(GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING) && (
            <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
              <ActionMenu.Item onClick={() => apentForPameldingModalRef.current?.showModal()}>
                {gjennomforing.apentForPamelding ? "Steng for påmelding" : "Åpne for påmelding"}
              </ActionMenu.Item>
            </AdministratorGuard>
          )}
        {handlinger.includes(GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => registrerStengtModalRef.current?.showModal()}>
              Registrer stengt hos arrangør
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(GjennomforingHandling.AVBRYT) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => setAvbrytModalOpen(true)}>
              Avbryt gjennomføring
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        <ActionMenu.Divider />
        {handlinger.includes(GjennomforingHandling.FORHANDSVIS_I_MODIA) && (
          <ActionMenu.Item
            as="a"
            href={`${previewArbeidsmarkedstiltakUrl()}/tiltak/${gjennomforing.id}`}
            target="_blank"
            icon={<ExternalLinkIcon aria-hidden />}
          >
            Forhåndsvis i Modia
          </ActionMenu.Item>
        )}
        {isGruppetiltak(gjennomforing) && handlinger.includes(GjennomforingHandling.DUPLISER) && (
          <ActionMenu.Item
            onClick={() => dupliserGjennomforing(gjennomforing)}
            icon={<LayersPlusIcon aria-hidden />}
          >
            Dupliser
          </ActionMenu.Item>
        )}
        {handlinger.includes(GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI) && (
          <ActionMenu.Item
            onClick={() => setGodkjennOkonomiModalOpen(true)}
            icon={<LayersPlusIcon aria-hidden />}
          >
            Godkjenn økonomi
          </ActionMenu.Item>
        )}
      </Handlinger>
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
      <GodkjennOkonomiModal
        open={godkjennOkonomiModalOpen}
        setOpen={setGodkjennOkonomiModalOpen}
        gjennomforingId={gjennomforing.id}
      />
    </KnapperadContainer>
  );
}

function GjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useGjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
