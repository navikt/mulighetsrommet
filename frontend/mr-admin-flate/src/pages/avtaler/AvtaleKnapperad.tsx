import { Avtale, NavAnsatt, Toggles } from "mulighetsrommet-api-client";
import { useAvtaleEndringshistorikk } from "@/api/avtaler/useAvtaleEndringshistorikk";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { BodyShort, Button, Dropdown } from "@navikt/ds-react";
import { useRef } from "react";
import { useNavigate } from "react-router-dom";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { VarselModal } from "@/components/modal/VarselModal";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { RegistrerOpsjonModal } from "@/components/avtaler/opsjoner/RegistrerOpsjonModal";
import { AvbrytAvtaleModal } from "@/components/modal/AvbrytAvtaleModal";
import { KnapperadContainer } from "@/pages/KnapperadContainer";

interface Props {
  bruker: NavAnsatt;
  avtale: Avtale;
}

export function AvtaleKnapperad({ bruker, avtale }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const registrerOpsjonModalRef = useRef<HTMLDialogElement>(null);
  const { data: registrereOpsjonIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_REGISTRERE_OPSJON,
  );

  function avtaleErAktiv(avtale: Avtale): boolean {
    return ["AKTIV"].includes(avtale.status.name);
  }

  function kanRegistrereOpsjon(avtale: Avtale): boolean {
    return (
      avtaleErAktiv(avtale) &&
      !!avtale?.opsjonsmodellData?.opsjonsmodell &&
      !!registrereOpsjonIsEnabled
    );
  }

  return (
    <KnapperadContainer>
      <EndringshistorikkPopover>
        <AvtaleEndringshistorikk id={avtale.id} />
      </EndringshistorikkPopover>
      <HarSkrivetilgang ressurs="Avtale">
        <Dropdown>
          <Button size="small" as={Dropdown.Toggle}>
            Handlinger
          </Button>
          <Dropdown.Menu>
            <Dropdown.Menu.GroupedList>
              <Dropdown.Menu.GroupedList.Item
                onClick={() => {
                  if (
                    avtale.administratorer &&
                    avtale.administratorer.length > 0 &&
                    !avtale.administratorer.map((a) => a.navIdent).includes(bruker.navIdent)
                  ) {
                    advarselModal.current?.showModal();
                  } else {
                    navigate("skjema");
                  }
                }}
              >
                Rediger
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
              {avtale && avtale.status.name === "AKTIV" && (
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    avbrytModalRef.current?.showModal();
                  }}
                >
                  Avbryt avtale
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
        headingText="Du er ikke eier av denne avtalen"
        body={<BodyShort>Vil du fortsette til redigeringen?</BodyShort>}
        secondaryButton
        primaryButton={
          <Button variant="primary" onClick={() => navigate("skjema")}>
            Ja, jeg vil redigere
          </Button>
        }
      />
      <AvbrytAvtaleModal modalRef={avbrytModalRef} avtale={avtale} />
      <RegistrerOpsjonModal modalRef={registrerOpsjonModalRef} avtale={avtale} />
    </KnapperadContainer>
  );
}

function AvtaleEndringshistorikk({ id }: { id: string }) {
  const historikk = useAvtaleEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
