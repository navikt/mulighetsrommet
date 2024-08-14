import { Button } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import { Opphav } from "@mr/api-client";
import { Lenkeknapp } from "mulighetsrommet-frontend-common/components/lenkeknapp/Lenkeknapp";
import { useState } from "react";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useMigrerteTiltakstyper } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import { inneholderUrl } from "@/utils/Utils";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";
import { OpprettTiltakIArenaModal } from "@/components/filter/OpprettTiltakIArenaModal";

export function TiltaksgjennomforingFilterButtons() {
  const { data: avtale } = useAvtale();
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyper();
  const [visKanIkkeOppretteTiltakModal, setVisKanIkkeOppretteTiltakModal] = useState(false);
  const setTiltaksgjennomforingFane = useSetAtom(gjennomforingDetaljerTabAtom);

  const visOpprettTiltaksgjennomforingKnapp = inneholderUrl("/avtaler/");

  const avtaleErOpprettetIAdminFlate = avtale?.opphav === Opphav.MR_ADMIN_FLATE;
  const avtaleErAftEllerVta = avtale?.tiltakstype?.tiltakskode
    ? ["ARBFORB", "VASV"].includes(avtale?.tiltakstype?.tiltakskode)
    : false;
  const avtalenErAktiv = avtale?.status.name === "AKTIV";

  const kanOppretteTiltak =
    avtale?.tiltakstype?.tiltakskode &&
    migrerteTiltakstyper?.includes(avtale.tiltakstype.tiltakskode);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-between",
        height: "100%",
        alignItems: "center",
      }}
    >
      {avtale && avtalenErAktiv && (
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            justifyContent: "end",
            gap: "1rem",
            alignItems: "center",
          }}
        >
          <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
            {visOpprettTiltaksgjennomforingKnapp && kanOppretteTiltak ? (
              <Lenkeknapp
                size="small"
                to={`skjema`}
                variant="primary"
                dataTestid="opprett-ny-tiltaksgjenomforing_knapp"
                onClick={() => setTiltaksgjennomforingFane("detaljer")}
              >
                Opprett ny tiltaksgjennomføring
              </Lenkeknapp>
            ) : (
              <Button
                variant="primary"
                size="small"
                onClick={() => setVisKanIkkeOppretteTiltakModal(true)}
              >
                Opprett ny tiltaksgjennomføring
              </Button>
            )}
            {avtaleErOpprettetIAdminFlate && avtaleErAftEllerVta && (
              <>
                <Button
                  size="small"
                  onClick={() => setModalOpen(true)}
                  variant="secondary"
                  type="button"
                  title="Legg til en eksisterende gjennomføring til avtalen"
                >
                  Koble gjennomføring til avtale
                </Button>
                <LeggTilGjennomforingModal
                  avtale={avtale}
                  modalOpen={modalOpen}
                  onClose={() => setModalOpen(false)}
                />
              </>
            )}
          </HarSkrivetilgang>
        </div>
      )}
      {avtale && (
        <OpprettTiltakIArenaModal
          open={visKanIkkeOppretteTiltakModal}
          onClose={() => setVisKanIkkeOppretteTiltakModal(false)}
          tiltakstype={avtale.tiltakstype.navn}
        />
      )}
    </div>
  );
}
