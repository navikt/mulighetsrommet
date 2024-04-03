import { useAtom, WritableAtom } from "jotai";
import { ArrangorTil } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingFilter } from "@/api/atoms";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@/utils/Utils";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "@/utils/filterUtils";
import { Filtertag, FiltertagsContainer, NavEnhetFiltertag } from "mulighetsrommet-frontend-common";

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
    <FiltertagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.search && (
        <Filtertag
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
        <NavEnhetFiltertag
          navEnheter={filter.navEnheter}
          onClose={() => setFilter({ ...filter, navEnheter: [] })}
        />
      )}
      {filter.tiltakstyper.map((tiltakstype) => (
        <Filtertag
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
        <Filtertag
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
        <Filtertag
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
        <Filtertag
          key={id}
          label={arrangorer?.find((arrangor) => arrangor.id === id)?.navn ?? id}
          onClose={() => {
            setFilter({
              ...filter,
              arrangorer: addOrRemove(filter.arrangorer, id),
            });
          }}
        />
      ))}
    </FiltertagsContainer>
  );
}
