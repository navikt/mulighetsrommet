import { Oppskrift } from "@/components/oppskrift/Oppskrift";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/hooks/useGetTiltaksgjennomforingIdFraUrl";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Alert, Tabs } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client";
import { ReactNode, Suspense, useState } from "react";
import SidemenyInfo from "@/components/sidemeny/SidemenyInfo";
import { TiltakDetaljer } from "@/components/tabs/TiltakDetaljer";
import { TiltakHeader } from "./TiltakHeader";
import styles from "./ViewTiltakDetaljer.module.scss";
import { useInnsatsgrupper } from "@/api/queries/useInnsatsgrupper";
import { EstimertVentetid } from "@/components/sidemeny/EstimertVentetid";
import { SidemenyKanKombineresMed } from "@/components/sidemeny/SidemenyKanKombineresMed";
import { DetaljerSkeleton } from "@mr/frontend-common";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  tiltak: VeilederflateTiltak;
  brukerActions: ReactNode;
  knapperad: ReactNode;
}

export function ViewTiltakDetaljer({ tiltak, brukerActions, knapperad }: Props) {
  const gjennomforingsId = useGetTiltaksgjennomforingIdFraUrl();
  const innsatsgrupper = useInnsatsgrupper();

  const [oppskriftId, setOppskriftId] = useState<string | undefined>(undefined);

  const harKombinasjon = tiltak.tiltakstype.kanKombineresMed.length > 0;

  if (!tiltak) {
    return (
      <Alert variant="warning">{`Det finnes ingen tiltaksgjennomføringer med id: "${gjennomforingsId}"`}</Alert>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.top_wrapper}>{knapperad}</div>
      <Suspense fallback={<DetaljerSkeleton />}>
        <>
          <div className={styles.tiltaksgjennomforing_detaljer} id="tiltaksgjennomforing_detaljer">
            <div className={styles.tiltakstype_header_maksbredde}>
              <TiltakHeader tiltak={tiltak} />
            </div>
            {isTiltakGruppe(tiltak) && !tiltak.apentForPamelding && (
              <div className={styles.apent_for_innsok_status}>
                <PadlockLockedFillIcon title="Tiltaket er stengt for innsøking" />
              </div>
            )}
            <div className={styles.sidemeny}>
              {isTiltakGruppe(tiltak) && tiltak.estimertVentetid && (
                <EstimertVentetid estimertVentetid={tiltak.estimertVentetid} />
              )}
              <Tabs size="small" defaultValue="info">
                <Tabs.List>
                  <Tabs.Tab value="info" label="Info" />
                  {harKombinasjon ? (
                    <Tabs.Tab value="kombineres" label="Kan kombineres med" />
                  ) : null}
                </Tabs.List>
                <Tabs.Panel value="info">
                  <SidemenyInfo tiltak={tiltak} innsatsgrupper={innsatsgrupper.data} />
                </Tabs.Panel>
                {harKombinasjon ? (
                  <Tabs.Panel value="kombineres">
                    <SidemenyKanKombineresMed tiltak={tiltak} />
                  </Tabs.Panel>
                ) : null}
              </Tabs>
              <div className={styles.brukeractions_container}>{brukerActions}</div>
            </div>
            <TiltakDetaljer tiltak={tiltak} setOppskriftId={setOppskriftId} />
          </div>
          <div className={styles.oppskriftContainer}>
            {oppskriftId && (
              <div className={styles.oppskrift_border}>
                <Oppskrift
                  oppskriftId={oppskriftId}
                  tiltakstypeId={tiltak.tiltakstype.sanityId}
                  setOppskriftId={setOppskriftId}
                />
              </div>
            )}
          </div>
        </>
      </Suspense>
    </div>
  );
}
