import { RegistrerOpsjonModal } from "@/components/avtaler/opsjoner/RegistrerOpsjonModal";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ActionMenu } from "@navikt/ds-react";
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
import { AdministratorGuard } from "@/components/handlinger/AdministratorGuard";
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
  const administratorer = avtale.administratorer.map((a) => a.navIdent);

  return (
    <KnapperadContainer>
      <EndringshistorikkPopover>
        <AvtaleEndringshistorikk id={avtale.id} />
      </EndringshistorikkPopover>
      <Handlinger>
        {handlinger.includes(AvtaleHandling.REDIGER) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => navigate(`/avtaler/${avtale.id}/rediger`)}>
              Rediger avtale
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(AvtaleHandling.REDIGER) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => navigate(`/avtaler/${avtale.id}/personvern/rediger`)}>
              Rediger personvern
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(AvtaleHandling.REDIGER) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item
              onClick={() => navigate(`/avtaler/${avtale.id}/veilederinformasjon/rediger`)}
            >
              Rediger informasjon for veiledere
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(AvtaleHandling.REGISTRER_OPSJON) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item
              onClick={() => {
                registrerOpsjonModalRef.current?.showModal();
              }}
            >
              Registrer opsjon
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(AvtaleHandling.OPPDATER_PRIS) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => setOppdaterPrisModalOpen(true)}>
              Oppdater pris
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(AvtaleHandling.OPPDATER_RAMMEDETALJER) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => setAvtaleModalOpen("Rammedetaljer")}>
              Oppdater rammedetaljer
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(AvtaleHandling.AVBRYT) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item onClick={() => setAvbrytModalOpen(true)}>
              Avbryt avtale
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        {handlinger.includes(AvtaleHandling.OPPRETT_GJENNOMFORING) && (
          <AdministratorGuard administratorer={administratorer} navIdent={ansatt.navIdent}>
            <ActionMenu.Item
              onClick={() => navigate(`/avtaler/${avtale.id}/opprett-gjennomforing`)}
            >
              Opprett ny gjennomføring
            </ActionMenu.Item>
          </AdministratorGuard>
        )}
        <ActionMenu.Divider />
        {handlinger.includes(AvtaleHandling.DUPLISER) && (
          <ActionMenu.Item onClick={dupliserAvtale}>
            <LayersPlusIcon fontSize="1.5rem" aria-label="Ikon for duplisering av dokument" />
            Dupliser
          </ActionMenu.Item>
        )}
      </Handlinger>
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
