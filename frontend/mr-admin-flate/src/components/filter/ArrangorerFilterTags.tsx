import { ArrangorerFilter } from "@/api/atoms";
import { useAtom, WritableAtom } from "jotai";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";

interface Props {
  filterAtom: WritableAtom<ArrangorerFilter, [newValue: ArrangorerFilter], void>;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function ArrangorerFilterTags({ filterAtom, filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.sok && (
        <FilterTag
          label={filter.sok}
          onClose={() => {
            setFilter({
              ...filter,
              sok: "",
            });
          }}
        />
      )}
    </FilterTagsContainer>
  );
}
