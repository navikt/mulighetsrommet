import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";
import { InnsendingFilterType } from "./filter";
import { useKostnadsstedFilter } from "@/api/enhet/useKostnadsstedFilter";
import { NavEnhetFilterTag } from "@/components/filter/NavEnhetFilterTag";

interface Props {
  filter: InnsendingFilterType;
  updateFilter: (values: Partial<InnsendingFilterType>) => void;
  tiltakstypeId?: string;
  filterOpen: boolean;
}

export function InnsendingFilterTags({ filter, updateFilter, tiltakstypeId, filterOpen }: Props) {
  const { data: kostnadssteder } = useKostnadsstedFilter();
  const { data: tiltakstyper } = useTiltakstyper();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={() => {}}>
      {filter.navEnheter.length > 0 && (
        <NavEnhetFilterTag
          navEnheter={filter.navEnheter}
          regioner={kostnadssteder}
          onClose={() => updateFilter({ navEnheter: [] })}
        />
      )}
      {!tiltakstypeId &&
        filter.tiltakstyper.map((tiltakstype) => (
          <FilterTag
            key={tiltakstype}
            label={tiltakstyper.find((t) => tiltakstype === t.id)?.navn || tiltakstype}
            onClose={() => {
              updateFilter({
                tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
              });
            }}
          />
        ))}
    </FilterTagsContainer>
  );
}
