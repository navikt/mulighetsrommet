import { ArrangorTil } from "@mr/api-client-v2";
import { GjennomforingFilterType } from "@/api/atoms";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterTag, FilterTagsContainer, NavEnhetFilterTag } from "@mr/frontend-common";

interface Props {
  filter: GjennomforingFilterType;
  updateFilter: (values: Partial<GjennomforingFilterType>) => void;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function GjennomforingFilterTags({
  filter,
  updateFilter,
  filterOpen,
  setTagsHeight,
}: Props) {
  const { data: arrangorer } = useArrangorer(ArrangorTil.TILTAKSGJENNOMFORING, {
    pageSize: 10000,
  });
  const { data: tiltakstyper } = useTiltakstyper();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.search && (
        <FilterTag
          label={`Søkt på: '${filter.search}'`}
          onClose={() => {
            updateFilter({
              search: "",
              page: 1,
            });
          }}
        />
      )}
      {filter.navEnheter.length > 0 && (
        <NavEnhetFilterTag
          navEnheter={filter.navEnheter.map((enhet) => enhet.navn)}
          onClose={() => updateFilter({ navEnheter: [], page: 1 })}
        />
      )}
      {filter.tiltakstyper.map((tiltakstype) => (
        <FilterTag
          key={tiltakstype}
          label={tiltakstyper.find((t) => tiltakstype === t.id)?.navn || tiltakstype}
          onClose={() => {
            updateFilter({
              tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
              page: 1,
            });
          }}
        />
      ))}
      {filter.statuser.map((status) => (
        <FilterTag
          key={status}
          label={
            TILTAKSGJENNOMFORING_STATUS_OPTIONS.find((o) => status === o.value)?.label || status
          }
          onClose={() => {
            updateFilter({
              statuser: addOrRemove(filter.statuser, status),
              page: 1,
            });
          }}
        />
      ))}
      {filter.visMineGjennomforinger && (
        <FilterTag
          label="Mine gjennomføringer"
          onClose={() => {
            updateFilter({
              visMineGjennomforinger: false,
              page: 1,
            });
          }}
        />
      )}
      {filter.arrangorer.map((id) => (
        <FilterTag
          key={id}
          label={arrangorer?.data.find((arrangor) => arrangor.id === id)?.navn ?? id}
          onClose={() => {
            updateFilter({
              arrangorer: addOrRemove(filter.arrangorer, id),
              page: 1,
            });
          }}
        />
      ))}
    </FilterTagsContainer>
  );
}
