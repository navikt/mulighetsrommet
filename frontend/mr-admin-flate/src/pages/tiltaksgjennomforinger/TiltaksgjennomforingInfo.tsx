import { Alert, Tabs } from "@navikt/ds-react";
import { TiltaksgjennomforingStatus } from "mulighetsrommet-api-client";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "../../components/laster/Laster";
import skjemaStyles from "../../components/skjema/Skjema.module.scss";
import styles from "../DetaljerInfo.module.scss";
import { TiltaksgjennomforingDetaljer } from "./TiltaksgjennomforingDetaljer";
import { TiltaksgjennomforingKnapperad } from "./TiltaksgjennomforingKnapperad";
import { RedaksjoneltInnholdPreview } from "../../components/redaksjonelt-innhold/RedaksjoneltInnholdPreview";
import { gjennomforingDetaljerTabAtom } from "../../api/atoms";
import { useAtom } from "jotai";
import { InlineErrorBoundary } from "../../ErrorBoundary";

export function TiltaksgjennomforingInfo() {
  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isLoading: isLoadingTiltaksgjennomforing,
  } = useTiltaksgjennomforingById();
  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  const { data: avtale, isLoading: isLoadingAvtale } = useAvtale(tiltaksgjennomforing?.avtaleId);

  if (isLoadingTiltaksgjennomforing || isLoadingAvtale) {
    return <Laster tekst="Laster informasjon om tiltaksgjennomføring..." />;
  }

  if (isErrorTiltaksgjennomforing) {
    return <Alert variant="error">Klarte ikke hente informasjon om tiltaksgjennomføring</Alert>;
  }

  if (!tiltaksgjennomforing) {
    return <Alert variant="warning">Fant ingen tiltaksgjennomføring</Alert>;
  }

  function visKnapperad(status: TiltaksgjennomforingStatus): boolean {
    const whitelist: TiltaksgjennomforingStatus[] = [
      TiltaksgjennomforingStatus.PLANLAGT,
      TiltaksgjennomforingStatus.GJENNOMFORES,
    ];

    return whitelist.includes(status);
  }

  return (
    <div className={styles.info_container}>
      <Tabs defaultValue={activeTab}>
        <Tabs.List className={skjemaStyles.tabslist}>
          <div>
            <Tabs.Tab onClick={() => setActiveTab("detaljer")} value="detaljer" label="Detaljer" />
            <Tabs.Tab
              onClick={() => setActiveTab("redaksjonelt_innhold")}
              value="redaksjonelt_innhold"
              label="Redaksjonelt innhold"
            />
          </div>
          {visKnapperad(tiltaksgjennomforing.status) && (
            <TiltaksgjennomforingKnapperad tiltaksgjennomforing={tiltaksgjennomforing} />
          )}
        </Tabs.List>
        <Tabs.Panel value="detaljer">
          <InlineErrorBoundary>
            <TiltaksgjennomforingDetaljer
              tiltaksgjennomforing={tiltaksgjennomforing}
              avtale={avtale}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
        <Tabs.Panel value="redaksjonelt_innhold">
          <InlineErrorBoundary>
            <RedaksjoneltInnholdPreview
              tiltakstypeId={tiltaksgjennomforing.tiltakstype.id}
              beskrivelse={tiltaksgjennomforing.beskrivelse}
              faneinnhold={tiltaksgjennomforing.faneinnhold}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
