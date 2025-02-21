import { useMutatePublisert } from "@/api/gjennomforing/useMutatePublisert";
import { useGjennomforingEndringshistorikk } from "@/api/gjennomforing/useGjennomforingEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { AvbrytGjennomforingModal } from "@/components/modal/AvbrytGjennomforingModal";
import { SetApentForPameldingModal } from "@/components/gjennomforing/SetApentForPameldingModal";
import { KnapperadContainer } from "@/pages/KnapperadContainer";
import { GjennomforingDto, NavAnsatt, Toggles } from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { BodyShort, Button, Dropdown, Switch } from "@navikt/ds-react";
import React, { useRef } from "react";
import { useNavigate, useRevalidator } from "react-router";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { RegistrerStengtHosArrangorModal } from "@/components/gjennomforing/stengt/RegistrerStengtHosArrangorModal";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

interface Props {
  ansatt: NavAnsatt;
  gjennomforing: GjennomforingDto;
}

export function GjennomforingKnapperad({ ansatt, gjennomforing }: Props) {
  const navigate = useNavigate();
  const { mutate } = useMutatePublisert();
  const revalidate = useRevalidator();
  const queryClient = useQueryClient();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const registrerStengtModalRef = useRef<HTMLDialogElement>(null);
  const apentForPameldingModalRef = useRef<HTMLDialogElement>(null);

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  function handleClick(e: React.MouseEvent<HTMLInputElement>) {
    mutate(
      { id: gjennomforing.id, publisert: e.currentTarget.checked },
      {
        onSuccess: async () => {
          await queryClient.invalidateQueries({
            queryKey: [QueryKeys.gjennomforing(gjennomforing.id)],
            refetchType: "all",
          });
          revalidate.revalidate();
        },
      },
    );
  }

  return (
    <KnapperadContainer>
      <HarSkrivetilgang
        ressurs="Gjennomføring"
        condition={gjennomforingIsAktiv(gjennomforing.status.status)}
      >
        <Switch checked={gjennomforing.publisert} onClick={handleClick}>
          Publiser
        </Switch>
      </HarSkrivetilgang>

      <EndringshistorikkPopover>
        <GjennomforingEndringshistorikk id={gjennomforing.id} />
      </EndringshistorikkPopover>

      <HarSkrivetilgang
        ressurs="Gjennomføring"
        condition={gjennomforingIsAktiv(gjennomforing.status.status)}
      >
        <Dropdown>
          <Button size="small" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            <Dropdown.Menu.GroupedList>
              <Dropdown.Menu.GroupedList.Item
                onClick={() => {
                  if (
                    gjennomforing.administratorer &&
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
              {gjennomforingIsAktiv(gjennomforing.status.status) && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => apentForPameldingModalRef.current?.showModal()}
                >
                  {gjennomforing.apentForPamelding ? "Steng for påmelding" : "Åpne for påmelding"}
                </Dropdown.Menu.GroupedList.Item>
              )}
              {enableOkonomi && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => registrerStengtModalRef.current?.showModal()}
                >
                  Registrer stengt hos arrangør
                </Dropdown.Menu.GroupedList.Item>
              )}
              {gjennomforingIsAktiv(gjennomforing.status.status) && (
                <Dropdown.Menu.GroupedList.Item onClick={() => avbrytModalRef.current?.showModal()}>
                  Avbryt gjennomføring
                </Dropdown.Menu.GroupedList.Item>
              )}
            </Dropdown.Menu.GroupedList>
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
