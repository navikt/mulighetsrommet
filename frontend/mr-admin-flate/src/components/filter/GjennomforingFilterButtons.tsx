import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { AvtaleDto, AvtaleStatus } from "@mr/api-client-v2";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { useSetAtom } from "jotai";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

interface Props {
  avtale?: AvtaleDto;
}

export function GjennomforingFilterButtons({ avtale }: Props) {
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
          </HarSkrivetilgang>
        </div>
      )}
    </div>
  );
}
