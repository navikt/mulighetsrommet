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

export function GjennomforingerForAvtalePage() {
  const id = useGetAvtaleIdFromUrlOrThrow();

  const filterAtomTiltaksgjennomforinger = gjennomforingerForAvtaleFilterAtomFamily(id);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const { data: avtale } = useAvtale();
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(gjennomforingfilterAtom);

  return (
    <>
      <FilterAndTableLayout
        filter={
          <GjennomforingFilter
            filterAtom={filterAtomTiltaksgjennomforinger}
            skjulFilter={{
              tiltakstype: true,
            }}
          />
        }
        lagredeFilter={
          <LagredeFilterOversikt
            setFilter={setFilter}
            filter={filter}
            dokumenttype={LagretDokumenttype.AVTALE}
            validateFilterStructure={(filter) => {
              return GjennomforingFilterSchema.safeParse(filter).success;
            }}
          />
        }
        tags={
          <GjennomforingFiltertags
            filterAtom={filterAtomTiltaksgjennomforinger}
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
            filterAtom={filterAtomTiltaksgjennomforinger}
            tagsHeight={tagsHeight}
            filterOpen={filterOpen}
          />
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        nullstillFilterButton={
          <NullstillKnappForGjennomforinger
            avtale={avtale}
            filterAtom={filterAtomTiltaksgjennomforinger}
          />
        }
      />
      <TilToppenKnapp />
    </>
  );
}
