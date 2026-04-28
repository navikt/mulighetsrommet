import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { InnsendingFilterType } from "./filter";
import { KostnadsstedFilterTag } from "@/components/filter/KostnadsstedFilterTag";
import { Chips } from "@navikt/ds-react";
import { TiltakstypeFilterTags } from "@/components/filter/TiltakstypeFilterTags";

interface Props {
  filter: InnsendingFilterType;
  updateFilter: (values: Partial<InnsendingFilterType>) => void;
  filterOpen: boolean;
}

export function InnsendingFilterTags({ filter, updateFilter, filterOpen }: Props) {
  const removeArrayItem = (key: keyof InnsendingFilterType, value: any) => {
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
        <TiltakstypeFilterTags
          ids={filter.tiltakstyper}
          onRemove={(tiltakstype) => removeArrayItem("tiltakstyper", tiltakstype)}
        />
      </Chips>
    </FilterTagsContainer>
  );
}
