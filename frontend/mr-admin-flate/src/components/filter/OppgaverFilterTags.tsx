import { OppgaverFilter } from "@/api/atoms";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { OPPGAVER_TYPE_STATUS } from "@/utils/filterUtils";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";
import { useAtom, WritableAtom } from "jotai";

interface Props {
  filterAtom: WritableAtom<OppgaverFilter, [newValue: OppgaverFilter], void>;
  tiltakstypeId?: string;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function OppgaveFilterTags({ filterAtom, tiltakstypeId, filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  const { data: enheter } = useNavEnheter();
  const { data: tiltakstyper } = useTiltakstyper();

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.type.map((type) => (
        <FilterTag
          key={type}
          label={OPPGAVER_TYPE_STATUS.find((o) => type === o.value)?.label || type}
          onClose={() => {
            setFilter({
              ...filter,
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
            setFilter({
              ...filter,
              regioner: addOrRemove(filter.regioner, enhetsnummer),
            });
          }}
        />
      ))}
      {!tiltakstypeId &&
        filter.tiltakstyper.map((tiltakstype) => (
          <FilterTag
            key={tiltakstype}
            label={
              tiltakstyper?.data?.find((t) => tiltakstype === t.tiltakskode)?.navn || tiltakstype
            }
            onClose={() => {
              setFilter({
                ...filter,
                tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
              });
            }}
          />
        ))}
    </FilterTagsContainer>
  );
}
