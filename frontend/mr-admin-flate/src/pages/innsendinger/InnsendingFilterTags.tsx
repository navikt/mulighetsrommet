import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { InnsendingFilterType } from "./filter";
import { useKostnadsstedFilter } from "@/api/enhet/useKostnadsstedFilter";
import { NavEnhetFilterTag } from "@/components/filter/NavEnhetFilterTag";
import { Chips } from "@navikt/ds-react";

interface Props {
  filter: InnsendingFilterType;
  updateFilter: (values: Partial<InnsendingFilterType>) => void;
  tiltakstypeId?: string;
  filterOpen: boolean;
}

export function InnsendingFilterTags({ filter, updateFilter, tiltakstypeId, filterOpen }: Props) {
  const { data: kostnadssteder } = useKostnadsstedFilter();
  const { data: tiltakstyper } = useTiltakstyper();

  const removeArrayItem = (key: keyof InnsendingFilterType, value: any) => {
    updateFilter({
      [key]: addOrRemove(filter[key] as any[], value),
    });
  };

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={() => {}}>
      <Chips>
        {filter.navEnheter.length > 0 && (
          <NavEnhetFilterTag
            navEnheter={filter.navEnheter}
            regioner={kostnadssteder}
            onClose={() => updateFilter({ navEnheter: [] })}
          />
        )}
        {!tiltakstypeId &&
          filter.tiltakstyper.map((tiltakstype) => (
            <Chips.Removable
              key={tiltakstype}
              onClick={() => removeArrayItem("tiltakstyper", tiltakstype)}
            >
              {tiltakstyper.find((t) => tiltakstype === t.id)?.navn || tiltakstype}
            </Chips.Removable>
          ))}
      </Chips>
    </FilterTagsContainer>
  );
}
