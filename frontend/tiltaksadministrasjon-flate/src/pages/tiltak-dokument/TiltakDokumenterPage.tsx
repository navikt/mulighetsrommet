import { TiltakDokumentTable } from "@/components/tiltak-dokument/TiltakDokumentTable";
import { TiltakDokumentFilter } from "@/components/tiltak-dokument/TiltakDokumentFilter";
import { TiltakDokumentFilterTags } from "@/components/tiltak-dokument/TiltakDokumentFilterTags";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { ListSkeleton, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Suspense, useState } from "react";
import { tiltakDokumentFilterStateAtom } from "@/pages/tiltak-dokument/filter";
import { useFilterState } from "@/filter/useFilterState";
import { TiltakDokumentIkon } from "@/components/ikoner/TiltakDokumentIkon";
import { Button } from "@navikt/ds-react";
import { PlusIcon } from "@navikt/aksel-icons";
import { useNavigate } from "react-router";

export function TiltakDokumenterPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);
  const { filter, updateFilter, resetToDefault, hasChanged } = useFilterState(
    tiltakDokumentFilterStateAtom,
  );
  const navigate = useNavigate();

  return (
    <>
      <title>Tiltaksdokumenter</title>
      <HeaderBanner heading="Oversikt over tiltaksdokumenter" ikon={<TiltakDokumentIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={<TiltakDokumentFilter filter={filter.values} updateFilter={updateFilter} />}
          nullstillFilterButton={
            hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null
          }
          tags={
            <TiltakDokumentFilterTags
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
              onClick={() => navigate("/tiltak-dokumenter/opprett")}
            >
              Opprett tiltaksdokument
            </Button>
          }

          table={
            <ReloadAppErrorBoundary>
              <Suspense fallback={<ListSkeleton />}>
                <TiltakDokumentTable
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
