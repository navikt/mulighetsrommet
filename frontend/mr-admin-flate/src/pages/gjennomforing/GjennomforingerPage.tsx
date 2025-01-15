import { gjennomforingfilterAtom, GjennomforingFilterSchema } from "@/api/atoms";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { useSlettFilter } from "@/api/lagret-filter/useSlettFilter";
import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { GjennomforingFiltertags } from "@/components/filter/GjennomforingFiltertags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { NullstillKnappForGjennomforinger } from "@/pages/gjennomforing/NullstillKnappForGjennomforinger";
import { LagretDokumenttype } from "@mr/api-client";
import { LagredeFilterOversikt, useOpenFilterWhenThreshold, useTitle } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useAtom } from "jotai/index";
import { useState } from "react";

export function GjennomforingerPage() {
  useTitle("Gjennomføringer");
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filter, setFilter] = useAtom(gjennomforingfilterAtom);
  const { data: lagredeFilter = [] } = useLagredeFilter(LagretDokumenttype.TILTAKSGJENNOMFØRING);
  const deleteFilterMutation = useSlettFilter(LagretDokumenttype.TILTAKSGJENNOMFØRING);

  return (
    <>
      <HeaderBanner heading="Oversikt over gjennomføringer" ikon={<GjennomforingIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={<GjennomforingFilter filterAtom={gjennomforingfilterAtom} />}
          lagredeFilter={
            <LagredeFilterOversikt
              setFilter={setFilter}
              lagredeFilter={lagredeFilter}
              deleteMutation={deleteFilterMutation}
              filter={filter}
              validateFilterStructure={(filter) => {
                return GjennomforingFilterSchema.safeParse(filter).success;
              }}
            />
          }
          tags={
            <GjennomforingFiltertags
              filterAtom={gjennomforingfilterAtom}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={<GjennomforingFilterButtons />}
          table={
            <ReloadAppErrorBoundary>
              <GjennomforingTable
                filterAtom={gjennomforingfilterAtom}
                tagsHeight={tagsHeight}
                filterOpen={filterOpen}
              />
            </ReloadAppErrorBoundary>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
          nullstillFilterButton={
            <NullstillKnappForGjennomforinger filterAtom={gjennomforingfilterAtom} />
          }
        />
      </ContentBox>
      <TilToppenKnapp />
    </>
  );
}
