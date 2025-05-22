import {
  gjennomforingerForAvtaleFilterAtomFamily,
  gjennomforingfilterAtom,
  GjennomforingFilterSchema,
} from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { NullstillKnappForGjennomforinger } from "@/pages/gjennomforing/NullstillKnappForGjennomforinger";
import { LagretFilterType } from "@mr/api-client-v2";
import { LagredeFilterOversikt, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";
import { useAvtale } from "@/api/avtaler/useAvtale";

export function GjennomforingerForAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

  const filterAtomGjennomforinger = gjennomforingerForAvtaleFilterAtomFamily(avtaleId);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(gjennomforingfilterAtom);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretFilterType.GJENNOMFORING);
  const deleteFilterMutation = useSlettFilter();

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
        lagredeFilter={
          <LagredeFilterOversikt
            setFilter={setFilter}
            filter={filter}
            lagredeFilter={lagredeFilter}
            onDelete={(id: string) => deleteFilterMutation.mutate(id)}
            validateFilterStructure={(filter) => {
              return GjennomforingFilterSchema.safeParse(filter).success;
            }}
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
