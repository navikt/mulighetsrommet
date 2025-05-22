import { gjennomforingerForAvtaleFilterAtomFamily } from "@/api/atoms";
import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { NullstillKnappForGjennomforinger } from "@/pages/gjennomforing/NullstillKnappForGjennomforinger";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { useAvtale } from "@/api/avtaler/useAvtale";

export function GjennomforingerForAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

  const filterAtomGjennomforinger = gjennomforingerForAvtaleFilterAtomFamily(avtaleId);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  return (
    <>
      <FilterAndTableLayout
        filter={
          <GjennomforingFilter
            filterAtom={filterAtomGjennomforinger}
            skjulFilter={{
              tiltakstype: true,
            }}
            avtale={avtale}
          />
        }
        tags={
          <GjennomforingFilterTags
            filterAtom={filterAtomGjennomforinger}
            filterOpen={filterOpen}
            setTagsHeight={setTagsHeight}
          />
        }
        buttons={<GjennomforingFilterButtons avtale={avtale} />}
        table={
          <GjennomforingTable
            skjulKolonner={{
              tiltakstype: true,
              arrangor: true,
            }}
            filterAtom={filterAtomGjennomforinger}
            tagsHeight={tagsHeight}
            filterOpen={filterOpen}
          />
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        nullstillFilterButton={
          <NullstillKnappForGjennomforinger
            avtale={avtale}
            filterAtom={filterAtomGjennomforinger}
          />
        }
      />
      <TilToppenKnapp />
    </>
  );
}
