import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { useFeatureToggles } from "../../../api/features/feature-toggles";
import { Tiltaksgjennomforingfilter } from "../../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingsTabell } from "../../../components/tabell/TiltaksgjennomforingsTabell";
import { TiltaksgjennomforingUtkast } from "../../../components/tiltaksgjennomforinger/TiltaksgjennomforingUtkast";
import { ErrorFallback } from "../../../main";

export function TiltaksgjennomforingerForAvtale() {
  const { data: features } = useFeatureToggles();

  const { data: avtale } = useAvtale();

  return (
    <>
      <Tabs defaultValue="gjennomforinger">
        <Tabs.List>
          <Tabs.Tab value="gjennomforinger" label="Gjennomføringer" />
          {features?.["mulighetsrommet.admin-flate-lagre-utkast"] &&
          features?.[
            "mulighetsrommet.admin-flate-opprett-tiltaksgjennomforing"
          ] ? (
            <Tabs.Tab
              data-testid="mine-utkast-tab"
              value="utkast"
              label="Mine utkast"
            />
          ) : null}
        </Tabs.List>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Tabs.Panel value="gjennomforinger">
            <Tiltaksgjennomforingfilter
              skjulFilter={{ tiltakstype: true }}
              avtale={avtale}
            />
            <TiltaksgjennomforingsTabell
              skjulKolonner={{
                tiltakstype: true,
                arrangor: true,
              }}
            />
          </Tabs.Panel>
          <Tabs.Panel value="utkast">
            <TiltaksgjennomforingUtkast />
          </Tabs.Panel>
        </ErrorBoundary>
      </Tabs>
    </>
  );
}
