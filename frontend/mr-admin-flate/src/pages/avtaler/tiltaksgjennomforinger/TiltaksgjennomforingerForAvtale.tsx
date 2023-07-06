import { useAvtale } from "../../../api/avtaler/useAvtale";
import { Tiltaksgjennomforingfilter } from "../../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingsTabell } from "../../../components/tabell/TiltaksgjennomforingsTabell";
import { useGetAvtaleIdFromUrl } from "../../../hooks/useGetAvtaleIdFromUrl";
import { Tabs } from "@navikt/ds-react";
import { TiltaksgjennomforingUtkast } from "../../../components/tiltaksgjennomforinger/TiltaksgjennomforingUtkast";
import { useFeatureToggles } from "../../../api/features/feature-toggles";

export function TiltaksgjennomforingerForAvtale() {
  const avtaleId = useGetAvtaleIdFromUrl();
  const { data: features } = useFeatureToggles();

  const { data: avtale } = useAvtale(avtaleId);

  return (
    <>
      <Tiltaksgjennomforingfilter
        skjulFilter={{ tiltakstype: true }}
        avtale={avtale}
      />

      <Tabs defaultValue="gjennomforinger">
        <Tabs.List>
          <Tabs.Tab value="gjennomforinger" label="Gjennomføringer" />
          {features?.["mulighetsrommet.admin-flate-lagre-utkast"] ? (
            <Tabs.Tab
              data-testid="mine-utkast-tab"
              value="utkast"
              label="Mine utkast"
            />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="gjennomforinger">
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
      </Tabs>
    </>
  );
}
