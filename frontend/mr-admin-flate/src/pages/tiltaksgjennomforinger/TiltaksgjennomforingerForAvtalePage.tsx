import { FilterAndTableLayout } from "../../components/filter/FilterAndTableLayout";
import { TiltaksgjennomforingFilter } from "../../components/filter/Tiltaksgjennomforingfilter";
import {
  gjennomforingPaginationAtom,
  tiltaksgjennomforingfilterForAvtaleAtom,
} from "../../api/atoms";
import { TiltaksgjennomforingFilterTags } from "../../components/filter/TiltaksgjennomforingFilterTags";
import { TiltaksgjennomforingFilterButtons } from "../../components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingsTabell } from "../../components/tabell/TiltaksgjennomforingsTabell";
import { useAtom } from "jotai";
import { useAdminTiltaksgjennomforinger } from "../../api/tiltaksgjennomforing/useAdminTiltaksgjennomforinger";

export function TiltaksgjennomforingerForAvtalePage() {
  const [page] = useAtom(gjennomforingPaginationAtom);
  const [filter] = useAtom(tiltaksgjennomforingfilterForAvtaleAtom);
  const { data, isLoading } = useAdminTiltaksgjennomforinger(filter, page);

  return (
    <div style={{ marginTop: "1rem" }}>
      <FilterAndTableLayout
        filter={
          <TiltaksgjennomforingFilter
            filterAtom={tiltaksgjennomforingfilterForAvtaleAtom}
            skjulFilter={{
              tiltakstype: true,
            }}
          />
        }
        tags={
          <TiltaksgjennomforingFilterTags filterAtom={tiltaksgjennomforingfilterForAvtaleAtom} />
        }
        buttons={
          <TiltaksgjennomforingFilterButtons filterAtom={tiltaksgjennomforingfilterForAvtaleAtom} />
        }
        table={
          <TiltaksgjennomforingsTabell
            skjulKolonner={{
              tiltakstype: true,
              arrangor: true,
            }}
            isLoading={isLoading}
            paginerteTiltaksgjennomforinger={data}
          />
        }
      />
    </div>
  );
}
