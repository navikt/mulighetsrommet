import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { TilsagnFilterType } from "@/pages/gjennomforing/tilsagn/filter";
import { Chips } from "@navikt/ds-react/Chips";
import { TILSAGN_STATUS_OPTIONS } from "@/utils/filterUtils";
import { KostnadsstedFilterTag } from "./KostnadsstedFilterTag";

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
      <Chips>
        {filter.kostnadssteder.length > 0 && (
          <KostnadsstedFilterTag
            kostnadssteder={filter.kostnadssteder}
            onClose={() => updateFilter({ kostnadssteder: [] })}
          />
        )}
        {filter.statuser.map((status) => (
          <Chips.Removable key={status} onClick={() => removeArrayItem("statuser", status)}>
            {TILSAGN_STATUS_OPTIONS.find((o) => status === o.value)?.label || status}
          </Chips.Removable>
        ))}
      </Chips>
    </FilterTagsContainer>
  );
}
