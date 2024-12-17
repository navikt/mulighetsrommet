import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { InfoContainer } from "@/components/skjema/InfoContainer";
import { InlineErrorBoundary } from "@mr/frontend-common";
import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { useLoaderData } from "react-router-dom";
import { TiltaksgjennomforingDetaljer } from "./TiltaksgjennomforingDetaljer";
import styles from "./TiltaksgjennomforingInfo.module.scss";
import { TiltaksgjennomforingKnapperad } from "./TiltaksgjennomforingKnapperad";
import { tiltaksgjennomforingLoader } from "./tiltaksgjennomforingLoaders";

export function TiltaksgjennomforingInfo() {
  const { tiltaksgjennomforing, ansatt, avtale } =
    useLoaderData<typeof tiltaksgjennomforingLoader>();
  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  if (!tiltaksgjennomforing) {
    return <Alert variant="warning">Fant ingen tiltaksgjennomføring</Alert>;
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
            ansatt={ansatt}
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
              tiltakstype={tiltaksgjennomforing.tiltakstype}
              beskrivelse={tiltaksgjennomforing.beskrivelse}
              faneinnhold={tiltaksgjennomforing.faneinnhold}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </InfoContainer>
  );
}
