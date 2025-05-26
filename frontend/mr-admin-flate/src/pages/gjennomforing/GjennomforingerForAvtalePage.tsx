import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useAtom } from "jotai";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { dequal } from "dequal";
import {
  getGjennomforingerForAvtaleFilterAtom,
  GjennomforingFilterType,
} from "@/pages/gjennomforing/filter";

export function GjennomforingerForAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

  const { defaultFilterValue, filterAtom } = getGjennomforingerForAvtaleFilterAtom(avtaleId);
  const [filter, setFilter] = useAtom(filterAtom);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  function updateFilter(value: Partial<GjennomforingFilterType>) {
    setFilter({ ...filter, ...value });
  }

  function resetFilter() {
    setFilter(defaultFilterValue);
  }

  const hasChanged = !dequal(filter, defaultFilterValue);

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
        nullstillFilterButton={hasChanged ? <NullstillFilterKnapp onClick={resetFilter} /> : null}
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
