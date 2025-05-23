import {
  defaultGjennomforingFilter,
  gjennomforingerForAvtaleFilterAtomFamily,
  GjennomforingFilterType,
} from "@/api/atoms";
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
import { useAtom } from "jotai";

export function GjennomforingerForAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

  const filterAtomGjennomforinger = gjennomforingerForAvtaleFilterAtomFamily(avtaleId);
  const [filter, setFilter] = useAtom(filterAtomGjennomforinger);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  function updateFilter(value: Partial<GjennomforingFilterType>) {
    setFilter({ ...filter, ...value });
  }

  function resetFilter() {
    setFilter({ ...defaultGjennomforingFilter, avtale: avtaleId });
  }

  return (
    <>
      <FilterAndTableLayout
        filter={
          <GjennomforingFilter
            filter={filter}
            updateFilter={updateFilter}
            skjulFilter={{
              tiltakstype: true,
            }}
            avtale={avtale}
          />
        }
        nullstillFilterButton={
          <NullstillKnappForGjennomforinger filter={filter} resetFilter={resetFilter} />
        }
        tags={
          <GjennomforingFilterTags
            filter={filter}
            updateFilter={updateFilter}
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
            filter={filter}
            updateFilter={updateFilter}
            tagsHeight={tagsHeight}
            filterOpen={filterOpen}
          />
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
      />
      <TilToppenKnapp />
    </>
  );
}
