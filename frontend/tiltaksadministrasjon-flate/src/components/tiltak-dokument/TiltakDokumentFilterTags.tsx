import { Chips } from "@navikt/ds-react";
import { FilterTagsContainer } from "@mr/frontend-common";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { TiltakDokumentFilterType } from "@/pages/tiltak-dokument/filter";
import { KontorstrukturFilterTag } from "@/components/filter/KontorstrukturFilterTag";
import { TiltakskodeFilterTags } from "@/components/filter/TiltakskodeFilterTags";

interface Props {
  filter: TiltakDokumentFilterType;
  updateFilter: (values: Partial<TiltakDokumentFilterType>) => void;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function TiltakDokumentFilterTags({
  filter,
  updateFilter,
  filterOpen,
  setTagsHeight,
}: Props) {
  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      <Chips>
        {filter.navEnheter.length > 0 && (
          <KontorstrukturFilterTag
            navEnheter={filter.navEnheter}
            onClick={() => updateFilter({ navEnheter: [], page: 1 })}
          />
        )}
        <TiltakskodeFilterTags
          tiltakskoder={filter.tiltakstyper}
          onRemove={(tiltakskode) =>
            updateFilter({
              tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakskode),
              page: 1,
            })
          }
        />
      </Chips>
    </FilterTagsContainer>
  );
}
