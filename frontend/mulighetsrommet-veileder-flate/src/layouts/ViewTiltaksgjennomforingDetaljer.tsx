import { Oppskrift } from "@/components/oppskrift/Oppskrift";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Alert } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { ReactNode, useState } from "react";
import SidemenyDetaljer from "../components/sidemeny/SidemenyDetaljer";
import TiltaksdetaljerFane from "../components/tabs/TiltaksdetaljerFane";
import TiltaksgjennomforingsHeader from "./TiltaksgjennomforingsHeader";
import styles from "./ViewTiltaksgjennomforingDetaljer.module.scss";
import { useInnsatsgrupper } from "@/api/queries/useInnsatsgrupper";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  brukerActions: ReactNode;
  knapperad: ReactNode;
}

export const ViewTiltaksgjennomforingDetaljer = ({
  tiltaksgjennomforing,
  brukerActions,
  knapperad,
}: Props) => {
  const gjennomforingsId = useGetTiltaksgjennomforingIdFraUrl();
  const innsatsgrupper = useInnsatsgrupper();

  const [oppskriftId, setOppskriftId] = useState<string | undefined>(undefined);

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med id: "${gjennomforingsId}"`}</Alert>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.top_wrapper}>{knapperad}</div>
      <div className={styles.tiltaksgjennomforing_detaljer} id="tiltaksgjennomforing_detaljer">
        <div className={styles.tiltakstype_header_maksbredde}>
          <TiltaksgjennomforingsHeader tiltaksgjennomforing={tiltaksgjennomforing} />
        </div>
        {!tiltaksgjennomforing.apentForInnsok && (
          <div className={styles.apent_for_innsok_status}>
            <PadlockLockedFillIcon title="Tiltaket er stengt for innsøking" />
          </div>
        )}
        <div className={styles.sidemeny}>
          <SidemenyDetaljer
            tiltaksgjennomforing={tiltaksgjennomforing}
            innsatsgrupper={innsatsgrupper.data}
          />
          <div className={styles.brukeractions_container}>{brukerActions}</div>
        </div>
        <TiltaksdetaljerFane
          tiltaksgjennomforing={tiltaksgjennomforing}
          setOppskriftId={setOppskriftId}
        />
      </div>
      <div className={styles.oppskriftContainer}>
        {oppskriftId && (
          <Oppskrift
            oppskriftId={oppskriftId}
            tiltakstypeId={tiltaksgjennomforing.tiltakstype.sanityId}
            setOppskriftId={setOppskriftId}
          />
        )}
      </div>
    </div>
  );
};
