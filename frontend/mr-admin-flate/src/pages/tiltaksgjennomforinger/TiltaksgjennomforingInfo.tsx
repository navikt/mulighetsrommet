import { Alert, Tabs } from "@navikt/ds-react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { Laster } from "@/components/laster/Laster";
import styles from "./TiltaksgjennomforingInfo.module.scss";
import { TiltaksgjennomforingDetaljer } from "./TiltaksgjennomforingDetaljer";
import { TiltaksgjennomforingKnapperad } from "./TiltaksgjennomforingKnapperad";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useAtom } from "jotai";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { InlineErrorBoundary } from "mulighetsrommet-frontend-common";
import { InfoContainer } from "@/components/skjema/InfoContainer";

export function TiltaksgjennomforingInfo() {
  const { data: bruker } = useHentAnsatt();

  const {
    data: tiltaksgjennomforing,
    isError: isErrorTiltaksgjennomforing,
    isPending: isLoadingTiltaksgjennomforing,
  } = useTiltaksgjennomforingById();

  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  const { data: avtale, isLoading: isLoadingAvtale } = useAvtale(tiltaksgjennomforing?.avtaleId);

  if (!bruker || isLoadingTiltaksgjennomforing || isLoadingAvtale) {
    return <Laster tekst="Laster informasjon om tiltaksgjennomføring..." />;
  }

  if (isErrorTiltaksgjennomforing) {
    return <Alert variant="error">Klarte ikke hente informasjon om tiltaksgjennomføring</Alert>;
  }

  return (
    <InfoContainer dataTestId="tiltaksgjennomforing_info-container">
      <Tabs defaultValue={activeTab}>
        <Tabs.List className={styles.tabslist}>
          <div>
            <Tabs.Tab onClick={() => setActiveTab("detaljer")} value="detaljer" label="Detaljer" />
            <Tabs.Tab
              onClick={() => setActiveTab("redaksjonelt-innhold")}
              value="redaksjonelt-innhold"
              label="Redaksjonelt innhold"
            />
          </div>
          <TiltaksgjennomforingKnapperad
            bruker={bruker}
            tiltaksgjennomforing={tiltaksgjennomforing}
          />
        </Tabs.List>
        <Tabs.Panel value="detaljer">
          <InlineErrorBoundary>
            <TiltaksgjennomforingDetaljer
              tiltaksgjennomforing={tiltaksgjennomforing}
              avtale={avtale}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
        <Tabs.Panel value="redaksjonelt-innhold">
          <InlineErrorBoundary>
            <RedaksjoneltInnholdPreview
              tiltakstypeId={tiltaksgjennomforing.tiltakstype.id}
              beskrivelse={tiltaksgjennomforing.beskrivelse}
              faneinnhold={tiltaksgjennomforing.faneinnhold}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </InfoContainer>
  );
}
