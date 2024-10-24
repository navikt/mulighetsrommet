import { Button } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import { Tiltakskode } from "@mr/api-client";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { useState } from "react";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";

export function TiltaksgjennomforingFilterButtons() {
  const { data: avtale } = useAvtale();
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const setTiltaksgjennomforingFane = useSetAtom(gjennomforingDetaljerTabAtom);

  const avtaleErAftEllerVta = avtale?.tiltakstype?.tiltakskode
    ? [
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
      ].includes(avtale.tiltakstype.tiltakskode)
    : false;
  const avtalenErAktiv = avtale?.status.name === "AKTIV";

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
            <Lenkeknapp
              size="small"
              to={`skjema`}
              variant="primary"
              dataTestid="opprett-ny-tiltaksgjenomforing_knapp"
              onClick={() => setTiltaksgjennomforingFane("detaljer")}
            >
              Opprett ny tiltaksgjennomføring
            </Lenkeknapp>
            {avtaleErAftEllerVta && (
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
    </div>
  );
}
