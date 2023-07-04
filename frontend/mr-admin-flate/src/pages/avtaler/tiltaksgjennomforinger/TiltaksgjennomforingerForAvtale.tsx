import { useAtom } from "jotai";
import { useEffect } from "react";
import { tiltaksgjennomforingfilter } from "../../../api/atoms";
import { useAvtale } from "../../../api/avtaler/useAvtale";
import { Tiltaksgjennomforingfilter } from "../../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingsTabell } from "../../../components/tabell/TiltaksgjennomforingsTabell";
import { useGetAvtaleIdFromUrl } from "../../../hooks/useGetAvtaleIdFromUrl";
import { Tabs } from "@navikt/ds-react";
import { TiltaksgjennomforingUtkast } from "../../../components/tiltaksgjennomforinger/TiltaksgjennomforingUtkast";

export function TiltaksgjennomforingerForAvtale() {
  const avtaleId = useGetAvtaleIdFromUrl();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilter);

  const { data: avtale } = useAvtale(avtaleId);

  useEffect(() => {
    if (avtaleId) {
      // For å filtrere på avtaler for den spesifikke tiltakstypen
      setFilter({ ...filter, avtale: avtaleId });
    }
  }, [avtaleId]);

  return (
    <>
      <Tiltaksgjennomforingfilter
        skjulFilter={{ tiltakstype: true }}
        avtale={avtale}
      />

      <Tabs defaultValue="gjennomforinger">
        <Tabs.List>
          <Tabs.Tab value="gjennomforinger" label="Tiltaksgjennomføringer" />
          <Tabs.Tab value="utkast" label="Mine utkast" />
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
