import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import {
  AvbrytGjennomforingAarsak,
  FieldError,
  GjennomforingDto,
  GjennomforingStatus,
  NavAnsatt,
  Opphav,
  ValidationError,
} from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import React, { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { useSetPublisert } from "@/api/gjennomforing/useSetPublisert";
import { useAvbrytGjennomforing } from "@/api/gjennomforing/useAvbrytGjennomforing";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useSuspenseGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";

interface Props {
  ansatt: NavAnsatt;
  gjennomforing: GjennomforingDto;
}

export function GjennomforingKnapperad({ ansatt, gjennomforing }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [avbrytModalErrors, setAvbrytModalErrors] = useState<FieldError[]>([]);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);
  const avbrytMutation = useAvbrytGjennomforing();
  const { data: deltakerSummary } = useSuspenseGjennomforingDeltakerSummary(gjennomforing.id);

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

  function avbryt(aarsaker: AvbrytGjennomforingAarsak[], forklaring: string | null) {
    avbrytMutation.mutate(
      {
        id: gjennomforing.id,
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
                  <Dropdown.Menu.GroupedList.Item
                    onClick={() => registrerStengtModalRef.current?.showModal()}
                  >
                    Registrer stengt hos arrangør
                  </Dropdown.Menu.GroupedList.Item>
                  <Dropdown.Menu.GroupedList.Item onClick={() => setAvbrytModalOpen(true)}>
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
      <AarsakerOgForklaringModal<AvbrytGjennomforingAarsak>
        header={`Ønsker du å avbryte «${gjennomforing.navn}»?`}
        open={avbrytModalOpen}
        buttonLabel="Ja, jeg vil avbryte gjennomføringen"
        ingress={
          deltakerSummary.antallDeltakere > 0 && (
            <Alert variant="warning">
              {`Det finnes ${deltakerSummary.antallDeltakere} deltaker${deltakerSummary.antallDeltakere > 1 ? "e" : ""} på gjennomføringen. Ved å
           avbryte denne vil det føre til statusendring på alle deltakere som har en aktiv status.`}
            </Alert>
          )
        }
        aarsaker={[
          { value: AvbrytGjennomforingAarsak.BUDSJETT_HENSYN, label: "Budsjetthensyn" },
          { value: AvbrytGjennomforingAarsak.ENDRING_HOS_ARRANGOR, label: "Endring hos arrangør" },
          { value: AvbrytGjennomforingAarsak.FEILREGISTRERING, label: "Feilregistrering" },
          { value: AvbrytGjennomforingAarsak.FOR_FAA_DELTAKERE, label: "For få deltakere" },
          { value: AvbrytGjennomforingAarsak.AVBRUTT_I_ARENA, label: "Avbrutt i Arena" },
          { value: AvbrytGjennomforingAarsak.ANNET, label: "Annet" },
        ]}
        onClose={() => {
          setAvbrytModalOpen(false);
          setAvbrytModalErrors([]);
        }}
        onConfirm={({ aarsaker, forklaring }) => avbryt(aarsaker, forklaring)}
        errors={avbrytModalErrors}
      />
    </KnapperadContainer>
  );
}

function GjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useGjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
