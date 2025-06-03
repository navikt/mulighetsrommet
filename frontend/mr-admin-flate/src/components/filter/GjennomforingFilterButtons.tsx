import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { AvtaleDto, AvtaleStatus, Avtaletype } from "@mr/api-client-v2";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { Button } from "@navikt/ds-react";
import { useSetAtom } from "jotai";
import { useState } from "react";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";

interface Props {
  avtale?: AvtaleDto;
}

export function GjennomforingFilterButtons({ avtale }: Props) {
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const setGjennomforingFane = useSetAtom(gjennomforingDetaljerTabAtom);

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
      {avtale?.status.type === AvtaleStatus.AKTIV && (
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            justifyContent: "end",
            gap: "1rem",
            alignItems: "center",
          }}
        >
          <HarSkrivetilgang ressurs="Gjennomføring">
            <Lenkeknapp
              size="small"
              to={`/avtaler/${avtale.id}/gjennomforinger/skjema`}
              variant="primary"
              dataTestid="opprett-ny-tiltaksgjenomforing_knapp"
              onClick={() => setGjennomforingFane("detaljer")}
            >
              Opprett ny gjennomføring
            </Lenkeknapp>
            {avtale.avtaletype === Avtaletype.FORHANDSGODKJENT && (
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
