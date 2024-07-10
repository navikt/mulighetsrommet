import { useAtom, WritableAtom } from "jotai";
import { ArrangorTil } from "mulighetsrommet-api-client";
import { AvtaleFilter } from "@/api/atoms";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove, avtaletypeTilTekst } from "@/utils/Utils";
import { AVTALE_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterTag, FilterTagsContainer } from "mulighetsrommet-frontend-common";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function AvtaleFiltertags({ filterAtom, tiltakstypeId, filterOpen, setTagsHeight }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  const { data: enheter } = useNavEnheter();
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: arrangorer } = useArrangorer(ArrangorTil.AVTALE, {
    pageSize: 10000,
  });

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.sok && (
        <FilterTag
          label={`Søkt på: '${filter.sok}'`}
          onClose={() => {
            setFilter({
              ...filter,
              sok: "",
            });
          }}
        />
      )}
      {filter.statuser.map((status) => (
        <FilterTag
          key={status}
          label={AVTALE_STATUS_OPTIONS.find((o) => status === o.value)?.label || status}
          onClose={() => {
            setFilter({
              ...filter,
              statuser: addOrRemove(filter.statuser, status),
            });
          }}
        />
      ))}
      {filter.avtaletyper.map((avtaletype) => (
        <FilterTag
          key={avtaletype}
          label={avtaletypeTilTekst(avtaletype)}
          onClose={() => {
            setFilter({
              ...filter,
              avtaletyper: addOrRemove(filter.avtaletyper, avtaletype),
            });
          }}
        />
      ))}
      {filter.visMineAvtaler && (
        <FilterTag
          label="Mine avtaler"
          onClose={() => {
            setFilter({
              ...filter,
              visMineAvtaler: false,
            });
          }}
        />
      )}
      {filter.navRegioner.map((enhetsnummer) => (
        <FilterTag
          key={enhetsnummer}
          label={enheter?.find((e) => e.enhetsnummer === enhetsnummer)?.navn || enhetsnummer}
          onClose={() => {
            setFilter({
              ...filter,
              navRegioner: addOrRemove(filter.navRegioner, enhetsnummer),
            });
          }}
        />
      ))}
      {!tiltakstypeId &&
        filter.tiltakstyper.map((tiltakstype) => (
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
      {filter.personvernBekreftet.map((p, i) => {
        return (
          <FilterTag
            key={i}
            label={p ? "Personvern bekreftet" : "Personvern ikke bekreftet"}
            onClose={() => {
              setFilter({ ...filter, personvernBekreftet: [] });
            }}
          />
        );
      })}
    </FilterTagsContainer>
  );
}
