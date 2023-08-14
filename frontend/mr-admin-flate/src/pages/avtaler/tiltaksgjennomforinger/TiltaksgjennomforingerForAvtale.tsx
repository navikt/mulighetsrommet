import { Tabs } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { Tiltaksgjennomforingfilter } from "../../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingsTabell } from "../../../components/tabell/TiltaksgjennomforingsTabell";
import { ErrorFallback } from "../../../main";
import { UtkastListe } from "../../../components/utkast/Utkastliste";

export function TiltaksgjennomforingerForAvtale() {
  const { data: avtale } = useAvtale();

  return (
    <>
      <Tabs defaultValue="gjennomforinger">
        <Tabs.List>
          <Tabs.Tab value="gjennomforinger" label="Gjennomføringer" />
          <Tabs.Tab
            data-testid="mine-utkast-tab"
            value="utkast"
            label="Mine utkast"
          />
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
            <UtkastListe dataType="gjennomforing" />
          </Tabs.Panel>
        </ErrorBoundary>
      </Tabs>
    </>
  );
}
