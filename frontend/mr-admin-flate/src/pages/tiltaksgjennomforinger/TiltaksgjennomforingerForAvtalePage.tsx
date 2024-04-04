import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TiltaksgjennomforingFilter } from "@/components/filter/TiltaksgjennomforingFilter";
import { gjennomforingerForAvtaleFilterAtomFamily } from "@/api/atoms";
import { TiltaksgjennomforingFiltertags } from "@/components/filter/TiltaksgjennomforingFiltertags";
import { TiltaksgjennomforingFilterButtons } from "@/components/filter/TiltaksgjennomforingFilterButtons";
import { TiltaksgjennomforingsTabell } from "@/components/tabell/TiltaksgjennomforingsTabell";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useState } from "react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { NullstillKnappForTiltaksgjennomforinger } from "@/pages/tiltaksgjennomforinger/NullstillKnappForTiltaksgjennomforinger";

export function TiltaksgjennomforingerForAvtalePage() {
  const id = useGetAvtaleIdFromUrlOrThrow();

  const filterAtom = gjennomforingerForAvtaleFilterAtomFamily(id);
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const { data: avtale } = useAvtale();
  const [tagsHeight, setTagsHeight] = useState(0);

  return (
    <FilterAndTableLayout
      filter={
        <TiltaksgjennomforingFilter
          filterAtom={filterAtom}
          skjulFilter={{
            tiltakstype: true,
          }}
        />
      }
      tags={
        <TiltaksgjennomforingFiltertags
          filterAtom={filterAtom}
          filterOpen={filterOpen}
          setTagsHeight={setTagsHeight}
        />
      }
      buttons={<TiltaksgjennomforingFilterButtons />}
      table={
        <TiltaksgjennomforingsTabell
          skjulKolonner={{
            tiltakstype: true,
            arrangor: true,
          }}
          filterAtom={filterAtom}
          tagsHeight={tagsHeight}
          filterOpen={filterOpen}
        />
      }
      filterOpen={filterOpen}
      setFilterOpen={setFilterOpen}
      nullstillFilterButton={
        <NullstillKnappForTiltaksgjennomforinger avtale={avtale} filterAtom={filterAtom} />
      }
    />
  );
}
