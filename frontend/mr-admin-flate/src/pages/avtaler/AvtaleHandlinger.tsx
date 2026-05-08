import { RegistrerOpsjonModal } from "@/components/avtaler/opsjoner/RegistrerOpsjonModal";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import {
  AvbrytAvtaleAarsak,
  AvtaleDto,
  AvtaleHandling,
  EndringshistorikkType,
  FieldError,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvbrytAvtale } from "@/api/avtaler/useAvbrytAvtale";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { OppdaterPrisModal } from "@/components/avtaler/OppdaterPrisModal";
import { useAvtaleHandlinger } from "@/api/avtaler/useAvtale";
import { OppdaterRammedetaljerModal } from "@/components/avtaler/OppdaterRammedetaljerModal";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { useEndringshistorikk } from "@/api/endringshistorikk/useEndringshistorikk";

interface Props {
  avtale: AvtaleDto;
}

type AvtaleModal = "Prismodell" | "Avbryt" | "Rammedetaljer";

export function AvtaleHandlinger({ avtale }: Props) {
  const navigate = useNavigate();
  const { data: handlinger } = useAvtaleHandlinger(avtale.id);
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [avbrytModalErrors, setAvbrytModalErrors] = useState<FieldError[]>([]);
  const registrerOpsjonModalRef = useRef<HTMLDialogElement>(null);
  const [oppdaterPrisModalOpen, setOppdaterPrisModalOpen] = useState<boolean>(false);
  const [avtaleModalOpen, setAvtaleModalOpen] = useState<AvtaleModal | null>(null);
  const { data: ansatt } = useHentAnsatt();
  const avbrytMutation = useAvbrytAvtale();
  const administratorer = avtale.administratorer.map((a) => a.navIdent);

  function dupliserAvtale() {
    navigate(`/avtaler/opprett`, {
      state: {
        dupliserAvtale: {
          tiltakstype: avtale.tiltakstype,
          avtaletype: avtale.avtaletype,
          beskrivelse: avtale.beskrivelse,
          faneinnhold: avtale.faneinnhold,
          opsjonsmodell: avtale.opsjonsmodell,
        },
      },
    });
  }

  function avbrytAvtale(aarsaker: AvbrytAvtaleAarsak[], forklaring: string | null) {
    avbrytMutation.mutate(
      {
        id: avtale.id,
        aarsaker,
        forklaring,
      },
      {
        onSuccess: () => {
          setAvbrytModalOpen(false);
        },
        onValidationError: (error: ValidationError) => {
          setAvbrytModalErrors(error.errors);
        },
      },
    );
  }

  return (
    <KnapperadContainer>
      <EndringshistorikkPopover>
        <AvtaleEndringshistorikk id={avtale.id} />
      </EndringshistorikkPopover>
      <Handlinger
        handlinger={handlinger}
        navIdent={ansatt.navIdent}
        grupper={[
          {
            label: "Avtale",
            items: [
              {
                label: "Rediger avtale",
                onClick: () => navigate(`/avtaler/${avtale.id}/rediger`),
                handling: AvtaleHandling.REDIGER,
                administratorer,
              },
              {
                label: "Rediger personvern",
                onClick: () => navigate(`/avtaler/${avtale.id}/personvern/rediger`),
                handling: AvtaleHandling.REDIGER,
                administratorer,
              },
              {
                label: "Rediger informasjon for veiledere",
                onClick: () => navigate(`/avtaler/${avtale.id}/veilederinformasjon/rediger`),
                handling: AvtaleHandling.REDIGER,
                administratorer,
              },
              {
                label: "Registrer opsjon",
                onClick: () => registrerOpsjonModalRef.current?.showModal(),
                handling: AvtaleHandling.REGISTRER_OPSJON,
                administratorer,
              },
              {
                label: "Avbryt avtale",
                onClick: () => setAvbrytModalOpen(true),
                handling: AvtaleHandling.AVBRYT,
                administratorer,
              },
              {
                label: "Dupliser",
                onClick: dupliserAvtale,
                icon: <LayersPlusIcon aria-hidden />,
                handling: AvtaleHandling.DUPLISER,
              },
            ],
          },
          {
            label: "Drift",
            items: [
              {
                label: "Oppdater pris",
                onClick: () => setOppdaterPrisModalOpen(true),
                handling: AvtaleHandling.OPPDATER_PRIS,
                administratorer,
              },
              {
                label: "Oppdater rammedetaljer",
                onClick: () => setAvtaleModalOpen("Rammedetaljer"),
                handling: AvtaleHandling.OPPDATER_RAMMEDETALJER,
                administratorer,
              },
            ],
          },
          {
            label: "Gjennomføringer",
            items: [
              {
                label: "Opprett ny gjennomføring",
                href: `/avtaler/${avtale.id}/opprett-gjennomforing`,
                handling: AvtaleHandling.OPPRETT_GJENNOMFORING,
              },
            ],
          },
        ]}
      />
      <AarsakerOgForklaringModal<AvbrytAvtaleAarsak>
        header="Ønsker du avbryte avtalen?"
        open={avbrytModalOpen}
        buttonLabel="Ja, jeg vil avbryte avtalen"
        aarsaker={[
          { value: AvbrytAvtaleAarsak.BUDSJETT_HENSYN, label: "Budsjett hensyn" },
          { value: AvbrytAvtaleAarsak.ENDRING_HOS_ARRANGOR, label: "Endring hos arrangør" },
          { value: AvbrytAvtaleAarsak.FEILREGISTRERING, label: "Feilregistrering" },
          { value: AvbrytAvtaleAarsak.AVBRUTT_I_ARENA, label: "Avbrutt i arena" },
          { value: AvbrytAvtaleAarsak.ANNET, label: "Annet" },
        ]}
        onClose={() => {
          setAvbrytModalOpen(false);
          setAvbrytModalErrors([]);
        }}
        onConfirm={({ aarsaker, forklaring }) => avbrytAvtale(aarsaker, forklaring)}
        errors={avbrytModalErrors}
      />
      <RegistrerOpsjonModal modalRef={registrerOpsjonModalRef} avtale={avtale} />
      {oppdaterPrisModalOpen && (
        <OppdaterPrisModal
          avtale={avtale}
          open={oppdaterPrisModalOpen}
          onClose={() => setOppdaterPrisModalOpen(false)}
        />
      )}
      {avtaleModalOpen === "Rammedetaljer" && (
        <OppdaterRammedetaljerModal avtaleId={avtale.id} onClose={() => setAvtaleModalOpen(null)} />
      )}
    </KnapperadContainer>
  );
}

function AvtaleEndringshistorikk({ id }: { id: string }) {
  const historikk = useEndringshistorikk(id, EndringshistorikkType.AVTALE);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
