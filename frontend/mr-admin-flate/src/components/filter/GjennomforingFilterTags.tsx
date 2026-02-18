import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { GjennomforingFilterType } from "@/pages/gjennomforing/filter";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { KontorstrukturFilterTag } from "@/components/filter/KontorstrukturFilterTag";
import { Chips } from "@navikt/ds-react";

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
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: arrangorer } = useArrangorer(ArrangorKobling.TILTAKSGJENNOMFORING, {
    pageSize: 10000,
  });

  const removeArrayItem = (key: keyof GjennomforingFilterType, value: any) => {
    updateFilter({
      [key]: addOrRemove(filter[key] as any[], value),
      page: 1,
    });
  };

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      <Chips>
        {filter.search && (
          <Chips.Removable onClick={() => updateFilter({ search: "", page: 1 })}>
            {`Søkt på: '${filter.search}'`}
          </Chips.Removable>
        )}
        {filter.navEnheter.length > 0 && (
          <KontorstrukturFilterTag
            navEnheter={filter.navEnheter}
            onClose={() => updateFilter({ navEnheter: [], page: 1 })}
          />
        )}
        {filter.tiltakstyper.map((tiltakstype) => (
          <Chips.Removable
            key={tiltakstype}
            onClick={() => removeArrayItem("tiltakstyper", tiltakstype)}
          >
            {tiltakstyper.find((t) => tiltakstype === t.id)?.navn || tiltakstype}
          </Chips.Removable>
        ))}
        {filter.statuser.map((status) => (
          <Chips.Removable key={status} onClick={() => removeArrayItem("statuser", status)}>
            {TILTAKSGJENNOMFORING_STATUS_OPTIONS.find((o) => status === o.value)?.label || status}
          </Chips.Removable>
        ))}
        {filter.visMineGjennomforinger && (
          <Chips.Removable onClick={() => updateFilter({ visMineGjennomforinger: false, page: 1 })}>
            Mine gjennomføringer
          </Chips.Removable>
        )}
        {filter.publisert.map((value) => (
          <Chips.Removable key={value} onClick={() => removeArrayItem("publisert", value)}>
            {value === "publisert" ? "Publisert" : "Ikke publisert"}
          </Chips.Removable>
        ))}
        {filter.arrangorer.map((id) => (
          <Chips.Removable key={id} onClick={() => removeArrayItem("arrangorer", id)}>
            {arrangorer?.data.find((arrangor) => arrangor.id === id)?.navn ?? id}
          </Chips.Removable>
        ))}
      </Chips>
    </FilterTagsContainer>
  );
}
