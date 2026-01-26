import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import React, { useRef, useState } from "react";
import { useNavigate } from "react-router";
import { useSetPublisert } from "@/api/gjennomforing/useSetPublisert";
import { useAvbrytGjennomforing } from "@/api/gjennomforing/useAvbrytGjennomforing";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import {
  ArenaMigreringOpphav,
  AvbrytGjennomforingAarsak,
  FieldError,
  GjennomforingDto,
  GjennomforingHandling,
  NavAnsattDto,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { parseDate } from "@mr/frontend-common/utils/date";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";

interface Props {
  ansatt: NavAnsattDto;
  gjennomforing: GjennomforingDto;
}

export function GjennomforingKnapperad({ ansatt, gjennomforing }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const { data: handlinger } = useGjennomforingHandlinger(gjennomforing.id);
  const [avbrytModalOpen, setAvbrytModalOpen] = useState<boolean>(false);
  const [avbrytModalErrors, setAvbrytModalErrors] = useState<FieldError[]>([]);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);
  const setGjennomforingDetaljerTab = useSetAtom(gjennomforingDetaljerTabAtom);
  const avbrytMutation = useAvbrytGjennomforing();
  const { data: deltakerSummary } = useGjennomforingDeltakerSummary(gjennomforing.id);

  const { mutate: setPublisert } = useSetPublisert(gjennomforing.id);

  async function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  function dupliserGjennomforing() {
    const duplisert: Partial<GjennomforingDto> = {
      opphav: ArenaMigreringOpphav.TILTAKSADMINISTRASJON,
      avtaleId: gjennomforing.avtaleId,
      beskrivelse: gjennomforing.beskrivelse,
      faneinnhold: gjennomforing.faneinnhold,
    };

    setGjennomforingDetaljerTab("detaljer");
    navigate(`/avtaler/${gjennomforing.avtaleId}/gjennomforinger/skjema`, {
      state: { dupliserGjennomforing: duplisert },
    });
  }

  function avbryt(
    aarsaker: AvbrytGjennomforingAarsak[],
    forklaring: string | null,
    dato: string | null,
  ) {
    avbrytMutation.mutate(
      {
        id: gjennomforing.id,
        aarsaker,
        forklaring,
        dato,
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
      {handlinger.includes(GjennomforingHandling.PUBLISER) && (
        <Switch name="publiser" checked={gjennomforing.publisert} onClick={togglePublisert}>
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
            {handlinger.includes(GjennomforingHandling.ENDRE_APEN_FOR_PAMELDING) && (
              <Dropdown.Menu.GroupedList.Item
                onClick={() => apentForPameldingModalRef.current?.showModal()}
              >
                {gjennomforing.apentForPamelding ? "Steng for påmelding" : "Åpne for påmelding"}
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
        gjennomforing={gjennomforing}
      />
      <SetApentForPameldingModal
        modalRef={apentForPameldingModalRef}
        gjennomforing={gjennomforing}
      />
      <AvbrytGjennomforingModal
        gjennomforing={gjennomforing}
        avbrytModalOpen={avbrytModalOpen}
        setAvbrytModalOpen={setAvbrytModalOpen}
        antallDeltakere={deltakerSummary.antallDeltakere}
        avbrytModalErrors={avbrytModalErrors}
        setAvbrytModalErrors={setAvbrytModalErrors}
        avbryt={avbryt}
      />
    </KnapperadContainer>
  );
}

function GjennomforingEndringshistorikk({ id }: { id: string }) {
  const historikk = useGjennomforingEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}

interface AvbrytGjennomforingModalProps {
  gjennomforing: GjennomforingDto;
  avbrytModalOpen: boolean;
  setAvbrytModalOpen: (b: boolean) => void;
  antallDeltakere: number;
  avbrytModalErrors: FieldError[];
  setAvbrytModalErrors: (e: FieldError[]) => void;
  avbryt: (
    aarsaker: AvbrytGjennomforingAarsak[],
    forklaring: string | null,
    dato: string | null,
  ) => void;
}

function AvbrytGjennomforingModal(props: AvbrytGjennomforingModalProps) {
  const [dato, setDato] = useState<string | null>(null);

  return (
    <AarsakerOgForklaringModal
      header={`Ønsker du å avbryte «${props.gjennomforing.navn}»?`}
      open={props.avbrytModalOpen}
      buttonLabel="Ja, jeg vil avbryte gjennomføringen"
      ingress={
        props.antallDeltakere > 0 && (
          <Alert variant="warning">
            {`Det finnes ${props.antallDeltakere} deltaker${props.antallDeltakere > 1 ? "e" : ""} på gjennomføringen. Ved å
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
        props.setAvbrytModalOpen(false);
        props.setAvbrytModalErrors([]);
      }}
      errors={props.avbrytModalErrors}
      extraFields={
        <ControlledDateInput
          label="Dato tiltaket ble avbrutt"
          fromDate={parseDate(props.gjennomforing.startDato)}
          toDate={new Date()}
          onChange={(val) => setDato(val)}
        />
      }
      onConfirm={({ aarsaker, forklaring }) => props.avbryt(aarsaker, forklaring, dato)}
    />
  );
}
