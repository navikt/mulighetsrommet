import { Oppskrift } from "@/components/oppskrift/Oppskrift";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Alert, Tabs } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client-v2";
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
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const tiltakId = useTiltakIdFraUrl();

  const [oppskriftId, setOppskriftId] = useState<string | undefined>(undefined);

  const harKombinasjon = tiltak.tiltakstype.kanKombineresMed.length > 0;

  if (!tiltak) {
    return <Alert variant="warning">{`Det finnes ingen tiltak med id: "${tiltakId}"`}</Alert>;
  }

  return (
    <div className="max-w-[1252px] mx-auto my-0">
      <div className="items-baseline flex justify-between">{knapperad}</div>
      <Suspense fallback={<DetaljerSkeleton />}>
        <>
          <div
            className="grid grid-rows-[auto_1fr] grid-cols-[auto] xl:grid-cols-[65%_35%] gap-[2rem] p-[2rem] bg-white"
            id="gjennomforing_detaljer"
          >
            <div className="max-w-none xl:max-w-[760px]">
              <TiltakHeader tiltak={tiltak} />
            </div>
            {isTiltakGruppe(tiltak) && !tiltak.apentForPamelding && (
              <div className="col-start-2 row-start-auto xl:row-start-1 text-2xl flex justify-end mr-8 text-[2rem]">
                <PadlockLockedFillIcon title="Tiltaket er stengt for påmelding" />
              </div>
            )}
            <div
              className={`${styles.sidemeny} mt-0 row-start max-w-none px-[3rem] xl:px-0 [grid-row-start:initial] [grid-row-end:initial] [grid-column:initial] xl:h-fit xl:row-start-1 xl:row-end-3 xl:col-start-2 xl:col-span-1 xl:max-w-[380px] text-text-default flex flex-col gap-[1rem]`}
            >
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
                  <SidemenyInfo tiltak={tiltak} innsatsgrupper={innsatsgrupper ?? []} />
                </Tabs.Panel>
                {harKombinasjon ? (
                  <Tabs.Panel value="kombineres">
                    <SidemenyKanKombineresMed tiltak={tiltak} />
                  </Tabs.Panel>
                ) : null}
              </Tabs>
              <div className="flex flex-col gap-[1rem] mt-[1rem]">{brukerActions}</div>
            </div>
            <TiltakDetaljer tiltak={tiltak} setOppskriftId={setOppskriftId} />
          </div>
          <div className="bg-white px-4 xl:px-8">
            {oppskriftId && (
              <div className="border border-border-subtle">
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
