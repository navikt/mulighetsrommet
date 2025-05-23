import { OppgaverFilterType } from "@/api/atoms";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";
import { useGetOppgavetyper } from "@/api/oppgaver/useGetOppgavetyper";

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
  const { data: enheter } = useNavEnheter();
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
          label={enheter?.find((e) => e.enhetsnummer === enhetsnummer)?.navn || enhetsnummer}
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
