import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { TilsagnFilterType } from "@/pages/gjennomforing/tilsagn/filter";
import { Chips } from "@navikt/ds-react/Chips";
import { TILSAGN_STATUS_OPTIONS } from "@/utils/filterUtils";
import { KostnadsstedFilterTag } from "./KostnadsstedFilterTag";
import type { FilterKilde } from "@/sporing/useUmami";
import type { TilsagnStatus } from "@tiltaksadministrasjon/api-client";

interface Props {
  filter: TilsagnFilterType;
  updateFilter: (values: Partial<TilsagnFilterType>, kilde?: FilterKilde) => void;
  filterOpen: boolean;
}

export function TilsagnFilterTags({ filter, updateFilter, filterOpen }: Props) {
  const removeStatus = (status: TilsagnStatus) => {
    updateFilter({
      statuser: addOrRemove(filter.statuser, status),
    });
  };

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={() => {}}>
      <Chips>
        {filter.kostnadssteder.length > 0 && (
          <KostnadsstedFilterTag
            kostnadssteder={filter.kostnadssteder}
            onClose={() => updateFilter({ kostnadssteder: [] })}
            sporingskontekst="tilsagn"
          />
        )}
        {filter.statuser.map((status) => (
          <Chips.Removable key={status} onClick={() => removeStatus(status)}>
            {TILSAGN_STATUS_OPTIONS.find((o) => status === o.value)?.label || status}
          </Chips.Removable>
        ))}
      </Chips>
    </FilterTagsContainer>
  );
}
