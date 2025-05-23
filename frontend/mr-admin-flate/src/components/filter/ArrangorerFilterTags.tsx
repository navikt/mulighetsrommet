import { ArrangorerFilterType } from "@/api/atoms";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";

interface Props {
  filter: ArrangorerFilterType;
  updateFilter: (values: Partial<ArrangorerFilterType>) => void;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function ArrangorerFilterTags({ filter, updateFilter, filterOpen, setTagsHeight }: Props) {
  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.sok && (
        <FilterTag
          label={filter.sok}
          onClose={() => {
            updateFilter({ sok: "" });
          }}
        />
      )}
    </FilterTagsContainer>
  );
}
