import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { TiltakstypeFilterTags } from "@/components/filter/TiltakstypeFilterTags";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { avtaletypeTilTekst } from "@/utils/Utils";
import { AVTALE_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { AvtaleFilterType } from "@/pages/avtaler/filter";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";
import { KontorstrukturFilterTag } from "@/components/filter/KontorstrukturFilterTag";
import { Chips } from "@navikt/ds-react";

interface Props {
  filter: AvtaleFilterType;
  updateFilter: (values: Partial<AvtaleFilterType>) => void;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function AvtaleFilterTags({ filter, updateFilter, filterOpen, setTagsHeight }: Props) {
  const { data: arrangorer } = useArrangorer(ArrangorKobling.AVTALE, {
    pageSize: 10000,
  });

  const removeArrayItem = (key: keyof AvtaleFilterType, value: any) => {
    updateFilter({
      [key]: addOrRemove(filter[key] as any[], value),
      page: 1,
    });
  };

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      <Chips>
        {filter.sok && (
          <Chips.Removable
            onClick={() => updateFilter({ sok: "", page: 1 })}
          >{`Søkt på: '${filter.sok}'`}</Chips.Removable>
        )}
        {filter.statuser.map((status) => (
          <Chips.Removable key={status} onClick={() => removeArrayItem("statuser", status)}>
            {AVTALE_STATUS_OPTIONS.find((o) => status === o.value)?.label || status}
          </Chips.Removable>
        ))}
        {filter.avtaletyper.map((avtaletype) => (
          <Chips.Removable
            key={avtaletype}
            onClick={() => removeArrayItem("avtaletyper", avtaletype)}
          >
            {avtaletypeTilTekst(avtaletype)}
          </Chips.Removable>
        ))}
        {filter.visMineAvtaler && (
          <Chips.Removable onClick={() => updateFilter({ visMineAvtaler: false, page: 1 })}>
            Mine avtaler
          </Chips.Removable>
        )}
        {filter.navEnheter.length > 0 && (
          <KontorstrukturFilterTag
            navEnheter={filter.navEnheter}
            onClick={() => updateFilter({ navEnheter: [], page: 1 })}
          />
        )}
        <TiltakstypeFilterTags
          ids={filter.tiltakstyper}
          onRemove={(tiltakstype) => removeArrayItem("tiltakstyper", tiltakstype)}
        />
        {filter.arrangorer.map((id) => (
          <Chips.Removable key={id} onClick={() => removeArrayItem("arrangorer", id)}>
            {arrangorer?.data.find((arrangor) => arrangor.id === id)?.navn ?? id}
          </Chips.Removable>
        ))}
        {(filter.personvernBekreftet === false || filter.personvernBekreftet === true) && (
          <Chips.Removable
            onClick={() => updateFilter({ personvernBekreftet: undefined, page: 1 })}
          >
            {filter.personvernBekreftet ? "Personvern bekreftet" : "Personvern ikke bekreftet"}
          </Chips.Removable>
        )}
      </Chips>
    </FilterTagsContainer>
  );
}
