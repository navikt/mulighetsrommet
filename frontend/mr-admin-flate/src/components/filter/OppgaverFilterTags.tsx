import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";
import { OppgaverFilterType } from "@/pages/oppgaveoversikt/oppgaver/filter";
import { useNavRegioner } from "@/api/enhet/useNavRegioner";

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
  const { data: regioner } = useNavRegioner();
  const { data: tiltakstyper } = useTiltakstyper();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.type.map((type) => (
        <FilterTag
          key={type}
          label={oppgavetyper.find((o) => type === o.type)?.navn || type}
          onClose={() => {
            updateFilter({
              type: addOrRemove(filter.type, type),
            });
          }}
        />
      ))}

      {filter.regioner.map((enhetsnummer) => (
        <FilterTag
          key={enhetsnummer}
          label={regioner.find((r) => r.enhetsnummer === enhetsnummer)?.navn || enhetsnummer}
          onClose={() => {
            updateFilter({
              regioner: addOrRemove(filter.regioner, enhetsnummer),
            });
          }}
        />
      ))}
      {!tiltakstypeId &&
        filter.tiltakstyper.map((tiltakstype) => (
          <FilterTag
            key={tiltakstype}
            label={tiltakstyper.find((t) => tiltakstype === t.tiltakskode)?.navn || tiltakstype}
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
