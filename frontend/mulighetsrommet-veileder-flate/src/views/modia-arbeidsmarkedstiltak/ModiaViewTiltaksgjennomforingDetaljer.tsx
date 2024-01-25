import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Alert } from "@navikt/ds-react";
import { Innsatsgruppe, VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { Outlet } from "react-router-dom";
import SidemenyDetaljer from "../../components/sidemeny/SidemenyDetaljer";
import TiltaksdetaljerFane from "../../components/tabs/TiltaksdetaljerFane";
import { useGetTiltaksgjennomforingIdFraUrl } from "../../core/api/queries/useGetTiltaksgjennomforingIdFraUrl";
import TiltaksgjennomforingsHeader from "../../layouts/TiltaksgjennomforingsHeader";
import styles from "./ModiaView.module.scss";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
  brukersInnsatsgruppe?: Innsatsgruppe;
  brukerActions: React.ReactNode;
  knapperad: React.ReactNode;
}

const ModiaViewTiltaksgjennomforingDetaljer = ({
  tiltaksgjennomforing,
  brukerActions,
  knapperad,
}: Props) => {
  const gjennomforingsId = useGetTiltaksgjennomforingIdFraUrl();

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
          <SidemenyDetaljer tiltaksgjennomforing={tiltaksgjennomforing} />
          {brukerActions}
        </div>
        <TiltaksdetaljerFane tiltaksgjennomforing={tiltaksgjennomforing} />
      </div>
      <div className={styles.oppskriftContainer}>
        <Outlet />
      </div>
    </div>
  );
};

export default ModiaViewTiltaksgjennomforingDetaljer;
