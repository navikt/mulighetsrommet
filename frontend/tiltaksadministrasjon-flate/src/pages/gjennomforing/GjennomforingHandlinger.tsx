import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { SetEstimertVentetidModal } from "@/components/gjennomforing/SetEstimertVentetidModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ArrowUndoIcon, ExternalLinkIcon, LayersPlusIcon } from "@navikt/aksel-icons";
import { Switch } from "@navikt/ds-react";
import React, { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { useSetPublisert } from "@/api/gjennomforing/useSetPublisert";
import {
  EndringshistorikkType,
  GjennomforingDetaljerDto,
  GjennomforingDto,
  GjennomforingHandling,
  GjennomforingVeilederinfoDto,
  NavAnsattDto,
} from "@tiltaksadministrasjon/api-client";
import { DeepPartial } from "react-hook-form";
import { AvbrytGjennomforingModal } from "@/components/gjennomforing/AvbrytGjennomforingModal";
import { GjenapneGjennomforingModal } from "@/components/gjennomforing/GjenapneGjennomforingModal";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { previewArbeidsmarkedstiltakUrl } from "@/constants";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useEndringshistorikk } from "@/api/endringshistorikk/useEndringshistorikk";

interface Props {
  ansatt: NavAnsattDto;
  gjennomforing: GjennomforingDto;
  veilederinfo: GjennomforingVeilederinfoDto | null;
  handlinger: GjennomforingHandling[];
}

export function GjennomforingHandlinger({
  ansatt,
  gjennomforing,
  veilederinfo,
  handlinger,
}: Props) {
  const navigate = useNavigate();
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [gjenapneModalOpen, setGjenapneModalOpen] = useState<boolean>(false);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);
  const [estimertVentetidModalOpen, setEstimertVentetidModalOpen] = useState(false);

  const { mutate: setPublisert } = useSetPublisert(gjennomforing.id);

  async function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  function dupliserGjennomforing() {
    if (!isGruppetiltak(gjennomforing)) {
      return;
    }

    const duplisert: DeepPartial<GjennomforingDetaljerDto> = {
      gjennomforing: {
        avtaleId: gjennomforing.avtaleId,
      },
      veilederinfo: {
        beskrivelse: veilederinfo?.beskrivelse,
        faneinnhold: veilederinfo?.faneinnhold,
      },
    };

    navigate(`/avtaler/${gjennomforing.avtaleId}/opprett-gjennomforing`, {
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
      <Handlinger
        handlinger={handlinger}
        navIdent={ansatt.navIdent}
        grupper={[
          {
            label: "Gjennomføring",
            items: [
              {
                label: "Rediger gjennomføring",
                onClick: () => navigate(`/gjennomforinger/${gjennomforing.id}/rediger`),
                handling: GjennomforingHandling.REDIGER,
                administratorer,
              },
              {
                label: "Rediger informasjon for veiledere",
                onClick: () =>
                  navigate(`/gjennomforinger/${gjennomforing.id}/redaksjonelt-innhold/rediger`),
                handling: GjennomforingHandling.REDIGER,
                administratorer,
              },
              {
                label: "Registrer stengt hos arrangør",
                onClick: () => registrerStengtModalRef.current?.showModal(),
                handling: GjennomforingHandling.REGISTRER_STENGT_HOS_ARRANGOR,
                administratorer,
              },
              {
                label: "Avbryt gjennomføring",
                onClick: () => setAvbrytModalOpen(true),
                handling: GjennomforingHandling.AVBRYT,
                administratorer,
              },
              {
                label: "Gjenåpne",
                onClick: () => setGjenapneModalOpen(true),
                icon: <ArrowUndoIcon aria-hidden />,
                handling: GjennomforingHandling.GJENAPNE,
                administratorer,
              },
              {
                label: "Dupliser",
                onClick: () => dupliserGjennomforing(),
                icon: <LayersPlusIcon aria-hidden />,
                handling: GjennomforingHandling.DUPLISER,
              },
            ],
          },
          ...(isGruppetiltak(gjennomforing)
            ? [
                {
                  label: "Oppfølging",
                  items: [
                    {
                      label: gjennomforing.apentForPamelding
                        ? "Steng for påmelding"
                        : "Åpne for påmelding",
                      onClick: () => apentForPameldingModalRef.current?.showModal(),
                      handling: GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING,
                      administratorer,
                    },
                    {
                      label: "Registrer estimert ventetid",
                      onClick: () => setEstimertVentetidModalOpen(true),
                      handling: GjennomforingHandling.REGISTRER_ESTIMERT_VENTETID,
                      administratorer,
                    },
                  ],
                },
              ]
            : []),
          {
            label: "Lenker",
            items: [
              {
                label: "Forhåndsvis i Modia",
                href: `${previewArbeidsmarkedstiltakUrl()}/tiltak/${gjennomforing.id}`,
                isExternal: true,
                icon: <ExternalLinkIcon aria-hidden />,
                handling: GjennomforingHandling.FORHANDSVIS_I_MODIA,
              },
            ],
          },
        ]}
      />
      {isGruppetiltak(gjennomforing) && (
        <RegistrerStengtHosArrangorModal
          modalRef={registrerStengtModalRef}
          gjennomforingId={gjennomforing.id}
          stengt={gjennomforing.stengt}
        />
      )}
      {isGruppetiltak(gjennomforing) && (
        <SetApentForPameldingModal
          modalRef={apentForPameldingModalRef}
          gjennomforingId={gjennomforing.id}
          apentForPamelding={gjennomforing.apentForPamelding}
        />
      )}
      <SetEstimertVentetidModal
        open={estimertVentetidModalOpen}
        setOpen={setEstimertVentetidModalOpen}
        gjennomforingId={gjennomforing.id}
        veilederinfo={veilederinfo}
      />
      <AvbrytGjennomforingModal
        open={avbrytModalOpen}
        setOpen={setAvbrytModalOpen}
        gjennomforingId={gjennomforing.id}
        gjennomforingNavn={gjennomforing.navn}
      />
      <GjenapneGjennomforingModal
        open={gjenapneModalOpen}
        setOpen={setGjenapneModalOpen}
        gjennomforingId={gjennomforing.id}
      />
    </KnapperadContainer>
  );
}

function GjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useEndringshistorikk(id, EndringshistorikkType.GJENNOMFORING);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
