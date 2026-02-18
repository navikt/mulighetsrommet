import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTagsContainer } from "@mr/frontend-common";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";
import { OppgaverFilterType } from "@/pages/oppgaveoversikt/oppgaver/filter";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { Chips } from "@navikt/ds-react";

interface Props {
  filter: OppgaverFilterType;
  updateFilter: (values: Partial<OppgaverFilterType>) => void;
  tiltakstypeId?: string;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function OppgaveFilterTags({
  filter,
  updateFilter,
  tiltakstypeId,
  filterOpen,
  setTagsHeight,
}: Props) {
  const { data: oppgavetyper } = useGetOppgavetyper();
  const { data: regioner } = useKontorstruktur();
  const { data: tiltakstyper } = useTiltakstyper();

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
        {!tiltakstypeId &&
          filter.tiltakstyper.map((tiltakstype) => (
            <Chips.Removable
              key={tiltakstype}
              onClick={() => removeArrayItem("tiltakstyper", tiltakstype)}
            >
              {tiltakstyper.find((t) => tiltakstype === t.tiltakskode)?.navn || tiltakstype}
            </Chips.Removable>
          ))}
      </Chips>
    </FilterTagsContainer>
  );
}
