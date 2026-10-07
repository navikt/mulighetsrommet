import { TiltakBeskrivelseTable } from "@/components/tiltak-beskrivelse/TiltakBeskrivelseTable";
import { TiltakBeskrivelseFilter } from "@/components/tiltak-beskrivelse/TiltakBeskrivelseFilter";
import { TiltakBeskrivelseFilterTags } from "@/components/tiltak-beskrivelse/TiltakBeskrivelseFilterTags";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { ListSkeleton, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Suspense, useState } from "react";
import { tiltakBeskrivelseFilterStateAtom } from "@/pages/tiltak-beskrivelse/filter";
import { useFilterState } from "@/filter/useFilterState";
import { TiltakBeskrivelseIkon } from "@/components/ikoner/TiltakBeskrivelseIkon";
import { Button } from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";
import { useNavigate } from "react-router";

export function TiltakBeskrivelserPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const { filter, updateFilter, resetToDefault, hasChanged } = useFilterState(
    tiltakBeskrivelseFilterStateAtom,
  );
  const navigate = useNavigate();

  return (
    <>
      <title>Tiltaksbeskrivelser</title>
      <HeaderBanner heading="Oversikt over tiltaksbeskrivelser" ikon={<TiltakBeskrivelseIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          hasChanged={hasChanged}
          filter={<TiltakBeskrivelseFilter filter={filter.values} updateFilter={updateFilter} />}
          nullstillFilterButton={<NullstillFilterKnapp onClick={resetToDefault} />}
          lagreFilterButton={null}
          tags={
            <TiltakBeskrivelseFilterTags
              filter={filter.values}
              updateFilter={updateFilter}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={
            <Button
              size="small"
              icon={<PlusIcon aria-hidden />}
              onClick={() => navigate("/tiltak-beskrivelser/opprett")}
            >
              Opprett tiltaksbeskrivelse
            </Button>
          }
          table={
            <ReloadAppErrorBoundary>
              <Suspense fallback={<ListSkeleton />}>
                <TiltakBeskrivelseTable
                  filter={filter.values}
                  updateFilter={updateFilter}
                  tagsHeight={tagsHeight}
                  filterOpen={filterOpen}
                />
              </Suspense>
            </ReloadAppErrorBoundary>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
      <TilToppenKnapp />
    </>
  );
}
