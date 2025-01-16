import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import {
  gjennomforingerForAvtaleFilterAtomFamily,
  gjennomforingfilterAtom,
  GjennomforingFilterSchema,
} from "@/api/atoms";
import { GjennomforingFiltertags } from "@/components/filter/GjennomforingFiltertags";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useState } from "react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { NullstillKnappForGjennomforinger } from "@/pages/gjennomforing/NullstillKnappForGjennomforinger";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { LagredeFilterOversikt, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { LagretDokumenttype } from "@mr/api-client";
import { useAtom } from "jotai/index";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";

export function GjennomforingerForAvtalePage() {
  const id = useGetAvtaleIdFromUrlOrThrow();

  const filterAtomGjennomforinger = gjennomforingerForAvtaleFilterAtomFamily(id);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const { data: avtale } = useAvtale();
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(gjennomforingfilterAtom);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretDokumenttype.GJENNOMFORING);
  const deleteFilterMutation = useSlettFilter(LagretDokumenttype.GJENNOMFORING);

  return (
    <>
      <FilterAndTableLayout
        filter={
          <GjennomforingFilter
            filterAtom={filterAtomGjennomforinger}
            skjulFilter={{
              tiltakstype: true,
            }}
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
          <GjennomforingFiltertags
            filterAtom={filterAtomGjennomforinger}
            filterOpen={filterOpen}
            setTagsHeight={setTagsHeight}
          />
        }
        buttons={<GjennomforingFilterButtons />}
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
