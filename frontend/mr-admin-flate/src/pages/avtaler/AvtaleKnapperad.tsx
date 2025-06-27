import { useAvtaleEndringshistorikk } from "@/api/avtaler/useAvtaleEndringshistorikk";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { RegistrerOpsjonModal } from "@/components/avtaler/opsjoner/RegistrerOpsjonModal";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { AvbrytAvtaleModal } from "@/components/modal/AvbrytAvtaleModal";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { BodyShort, Button, Dropdown } from "@navikt/ds-react";
import { AvtaleDto, NavAnsatt, Opphav, AvtaleStatus } from "@mr/api-client-v2";
import { useRef } from "react";
import { useNavigate } from "react-router";
import { LayersPlusIcon } from "@navikt/aksel-icons";
import { useSetAtom } from "jotai";
import { avtaleDetaljerTabAtom } from "@/api/atoms";

interface Props {
  ansatt: NavAnsatt;
  avtale: AvtaleDto;
}

export function AvtaleKnapperad({ ansatt, avtale }: Props) {
  const navigate = useNavigate();
  const advarselModal = useRef<HTMLDialogElement>(null);
  const avbrytModalRef = useRef<HTMLDialogElement>(null);
  const registrerOpsjonModalRef = useRef<HTMLDialogElement>(null);
  const setAvtaleDetaljerTab = useSetAtom(avtaleDetaljerTabAtom);

  function kanRegistrereOpsjon(avtale: AvtaleDto): boolean {
    return !!avtale.opsjonsmodell.opsjonMaksVarighet;
  }

  function dupliserAvtale() {
    setAvtaleDetaljerTab("detaljer");
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
                <Dropdown.Menu.GroupedList.Item
                  onClick={() => {
                    avbrytModalRef.current?.showModal();
                  }}
                >
                  Avbryt avtale
                </Dropdown.Menu.GroupedList.Item>
              )}
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
      <AvbrytAvtaleModal modalRef={avbrytModalRef} avtale={avtale} />
      <RegistrerOpsjonModal modalRef={registrerOpsjonModalRef} avtale={avtale} />
    </KnapperadContainer>
  );
}

function AvtaleEndringshistorikk({ id }: { id: string }) {
  const historikk = useAvtaleEndringshistorikk(id);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
