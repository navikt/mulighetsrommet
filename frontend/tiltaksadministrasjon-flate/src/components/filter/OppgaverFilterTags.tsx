import { TiltakskodeFilterTags } from "@/components/filter/TiltakskodeFilterTags";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";
import { OppgaverFilterType } from "@/pages/oppgaveoversikt/oppgaver/filter";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { Chips } from "@navikt/ds-react";
import { useArrangorer } from "@/api/arrangor/useArrangorer";

interface Props {
  filter: OppgaverFilterType;
  updateFilter: (values: Partial<OppgaverFilterType>) => void;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function OppgaveFilterTags({ filter, updateFilter, filterOpen, setTagsHeight }: Props) {
  const { data: oppgavetyper } = useGetOppgavetyper();
  const { data: regioner } = useKontorstruktur();
  const { data: arrangorer } = useArrangorer(undefined, {
    pageSize: 10000,
  });

  const removeArrayItem = (key: keyof OppgaverFilterType, value: any) => {
    updateFilter({
      [key]: addOrRemove(filter[key] as any[], value),
    });
  };

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      <Chips>
        {filter.type.map((type) => (
          <Chips.Removable key={type} onClick={() => removeArrayItem("type", type)}>
            {oppgavetyper.find((o) => type === o.type)?.navn || type}
          </Chips.Removable>
        ))}
        {filter.regioner.map((enhetsnummer) => (
          <Chips.Removable
            key={enhetsnummer}
            onClick={() => removeArrayItem("regioner", enhetsnummer)}
          >
            {regioner.find(({ region }) => region.enhetsnummer === enhetsnummer)?.region.navn ||
              enhetsnummer}
          </Chips.Removable>
        ))}
        {filter.arrangorer.map((id) => (
          <Chips.Removable key={id} onClick={() => removeArrayItem("arrangorer", id)}>
            {arrangorer?.data.find((arrangor) => arrangor.id === id)?.navn ?? id}
          </Chips.Removable>
        ))}
        <TiltakskodeFilterTags
          tiltakskoder={filter.tiltakstyper}
          onRemove={(tiltakstype) => removeArrayItem("tiltakstyper", tiltakstype)}
        />
      </Chips>
    </FilterTagsContainer>
  );
}
