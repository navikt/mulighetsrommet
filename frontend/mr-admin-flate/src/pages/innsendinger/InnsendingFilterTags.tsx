import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";
import { InnsendingFilterType } from "./filter";

interface Props {
  filter: InnsendingFilterType;
  updateFilter: (values: Partial<InnsendingFilterType>) => void;
  tiltakstypeId?: string;
  filterOpen: boolean;
}

export function InnsendingFilterTags({ filter, updateFilter, tiltakstypeId, filterOpen }: Props) {
  const { data: enheter } = useNavEnheter();
  const { data: tiltakstyper } = useTiltakstyper();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={() => {}}>
      {filter.navEnheter.map((enhetsnummer) => (
        <FilterTag
          key={enhetsnummer}
          label={enheter.find((e) => e.enhetsnummer === enhetsnummer)?.navn || enhetsnummer}
          onClose={() => {
            updateFilter({
              navEnheter: addOrRemove(filter.navEnheter, enhetsnummer),
            });
          }}
        />
      ))}
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
