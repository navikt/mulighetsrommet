import { useAtom, WritableAtom } from "jotai";
import { ArrangorTil } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingFilter } from "@/api/atoms";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@/utils/Utils";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterTag, FilterTagsContainer, NavEnhetFilterTag } from "mulighetsrommet-frontend-common";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilter,
    [newValue: TiltaksgjennomforingFilter],
    void
  >;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function TiltaksgjennomforingFiltertags({ filterAtom, filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const { data: arrangorer } = useArrangorer(ArrangorTil.TILTAKSGJENNOMFORING);
  const { data: tiltakstyper } = useTiltakstyper();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.search && (
        <FilterTag
          label={filter.search}
          onClose={() => {
            setFilter({
              ...filter,
              search: "",
            });
          }}
        />
      )}
      {filter.navEnheter.length > 0 && (
        <NavEnhetFilterTag
          navEnheter={filter.navEnheter}
          onClose={() => setFilter({ ...filter, navEnheter: [] })}
        />
      )}
      {filter.tiltakstyper.map((tiltakstype) => (
        <FilterTag
          key={tiltakstype}
          label={tiltakstyper?.data?.find((t) => tiltakstype === t.id)?.navn || tiltakstype}
          onClose={() => {
            setFilter({
              ...filter,
              tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
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
            setFilter({
              ...filter,
              statuser: addOrRemove(filter.statuser, status),
            });
          }}
        />
      ))}
      {filter.visMineGjennomforinger && (
        <FilterTag
          label="Mine gjennomføringer"
          onClose={() => {
            setFilter({
              ...filter,
              visMineGjennomforinger: false,
            });
          }}
        />
      )}
      {filter.arrangorer.map((id) => (
        <FilterTag
          key={id}
          label={arrangorer?.data.find((arrangor) => arrangor.id === id)?.navn ?? id}
          onClose={() => {
            setFilter({
              ...filter,
              arrangorer: addOrRemove(filter.arrangorer, id),
            });
          }}
        />
      ))}
    </FilterTagsContainer>
  );
}
