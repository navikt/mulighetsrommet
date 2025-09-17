import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { getGjennomforingerForAvtaleFilterAtom } from "@/pages/gjennomforing/filter";
import { useFilterState } from "@/filter/useFilterState";

export function GjennomforingerForAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const filterAtom = getGjennomforingerForAvtaleFilterAtom(avtaleId);
  const { filter, updateFilter, resetToDefault, hasChanged } = useFilterState(filterAtom);

  return (
    <>
      <FilterAndTableLayout
        filter={
          <GjennomforingFilter
            filter={filter.values}
            updateFilter={updateFilter}
            skjulFilter={{
              tiltakstype: true,
            }}
            avtale={avtale}
          />
        }
        nullstillFilterButton={
          hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null
        }
        tags={
          <GjennomforingFilterTags
            filter={filter.values}
            updateFilter={updateFilter}
            filterOpen={filterOpen}
            setTagsHeight={setTagsHeight}
          />
        }
        buttons={null}
        table={
          <GjennomforingTable
            skjulKolonner={{
              tiltakstype: true,
              arrangor: true,
            }}
            filter={filter.values}
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
