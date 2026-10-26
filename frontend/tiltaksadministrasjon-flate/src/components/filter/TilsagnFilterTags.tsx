import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { TilsagnFilterType } from "@/pages/gjennomforing/tilsagn/filter";

interface Props {
  filter: TilsagnFilterType;
  updateFilter: (values: Partial<TilsagnFilterType>) => void;
  filterOpen: boolean;
}

export function TilsagnFilterTags({ filter, updateFilter, filterOpen }: Props) {
  const removeArrayItem = (key: keyof TilsagnFilterType, value: any) => {
    updateFilter({
      [key]: addOrRemove(filter[key] as any[], value),
    });
  };

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={() => {}}>
      <div></div>
    </FilterTagsContainer>
  );
}
