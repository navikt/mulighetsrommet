import { useAtom } from "jotai";
import { useEffect } from "react";
import { tiltaksgjennomforingfilter } from "../../../api/atoms";
import { useGetAvtaleIdFromUrl } from "../../../hooks/useGetAvtaleIdFromUrl";
import { Tiltaksgjennomforingfilter } from "../../../components/filter/Tiltaksgjennomforingfilter";
import { TiltaksgjennomforingsTabell } from "../../../components/tabell/TiltaksgjennomforingsTabell";
import { useAdminTiltaksgjennomforinger } from "../../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";
import { useAvtale } from "../../../api/avtaler/useAvtale";

export function TiltaksgjennomforingerForAvtale() {
  const avtaleId = useGetAvtaleIdFromUrl();
  const { data } = useAdminTiltaksgjennomforinger();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilter);

  const { data: avtale } = useAvtale(avtaleId);

  useEffect(() => {
    if (avtaleId) {
      // For å filtrere på avtaler for den spesifikke tiltakstypen
      setFilter({ ...filter, avtale: avtaleId });
    }
  }, [avtaleId]);

  if (!data || !avtale) {
    return null;
  }

  return (
    <>
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
    </>
  );
}
