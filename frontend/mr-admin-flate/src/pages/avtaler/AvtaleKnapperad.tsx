import { useAvtaleEndringshistorikk } from "@/api/avtaler/useAvtaleEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { RegistrerOpsjonModal } from "@/components/avtaler/opsjoner/RegistrerOpsjonModal";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { BodyShort, Button, Dropdown } from "@navikt/ds-react";
import {
  AvtaleDto,
  Opphav,
  AvtaleStatus,
  AvbrytAvtaleAarsak,
  ValidationError,
  FieldError,
} from "@mr/api-client-v2";
import { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvbrytAvtale } from "@/api/avtaler/useAvbrytAvtale";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";

interface Props {
  avtale: AvtaleDto;
}

export function AvtaleKnapperad({ avtale }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [avbrytModalErrors, setAvbrytModalErrors] = useState<FieldError[]>([]);
  const registrerOpsjonModalRef = useRef<HTMLDialogElement>(null);
  const { data: ansatt } = useHentAnsatt();
  const avbrytMutation = useAvbrytAvtale();

  function kanRegistrereOpsjon(avtale: AvtaleDto): boolean {
    return !!avtale.opsjonsmodell.opsjonMaksVarighet;
  }

  function dupliserAvtale() {
    navigate(`/avtaler/skjema`, {
      state: {
        dupliserAvtale: {
          opphav: Opphav.TILTAKSADMINISTRASJON,
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
      <HarSkrivetilgang ressurs="Avtale">
        <Dropdown>
          <Button size="small" variant="secondary" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            <Dropdown.Menu.GroupedList>
              <Dropdown.Menu.GroupedList.Item
                onClick={() => {
                  if (
                    avtale.administratorer &&
                    avtale.administratorer.length > 0 &&
                    !avtale.administratorer.map((a) => a.navIdent).includes(ansatt.navIdent)
                  ) {
                    advarselModal.current?.showModal();
                  } else {
                    navigate("skjema");
                  }
                }}
              >
                Rediger avtale
              </Dropdown.Menu.GroupedList.Item>
              {kanRegistrereOpsjon(avtale) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    registrerOpsjonModalRef.current?.showModal();
                  }}
                >
                  Registrer opsjon
                </Dropdown.Menu.GroupedList.Item>
              )}
              {avtale.status.type === AvtaleStatus.AKTIV && (
                <Dropdown.Menu.GroupedList.Item onClick={() => setAvbrytModalOpen(true)}>
                  Avbryt avtale
                </Dropdown.Menu.GroupedList.Item>
              )}
              <Dropdown.Menu.GroupedList.Item
                onClick={() => navigate(`/avtaler/${avtale.id}/gjennomforinger/skjema`)}
              >
                Opprett ny gjennomføring
              </Dropdown.Menu.GroupedList.Item>
            </Dropdown.Menu.GroupedList>
            <Dropdown.Menu.Divider />
            <Dropdown.Menu.List>
              <Dropdown.Menu.List.Item onClick={dupliserAvtale}>
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
        headingText="Du er ikke eier av denne avtalen"
        body={<BodyShort>Vil du fortsette til redigeringen?</BodyShort>}
        secondaryButton
        primaryButton={
          <Button variant="primary" onClick={() => navigate("skjema")}>
            Ja, jeg vil redigere
          </Button>
        }
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
    </KnapperadContainer>
  );
}

function AvtaleEndringshistorikk({ id }: { id: string }) {
  const historikk = useAvtaleEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
