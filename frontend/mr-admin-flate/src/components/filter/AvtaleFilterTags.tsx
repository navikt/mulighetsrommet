import { useAtom, WritableAtom } from "jotai";
import { ArrangorTil } from "@mr/api-client-v2";
import { AvtaleFilterType } from "@/api/atoms";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { avtaletypeTilTekst } from "@/utils/Utils";
import { AVTALE_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";

interface Props {
  filterAtom: WritableAtom<AvtaleFilterType, [newValue: AvtaleFilterType], void>;
  tiltakstypeId?: string;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function AvtaleFilterTags({ filterAtom, tiltakstypeId, filterOpen, setTagsHeight }: Props) {
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
              page: 1,
              lagretFilterIdValgt: undefined,
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
              page: 1,
              lagretFilterIdValgt: undefined,
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
              page: 1,
              lagretFilterIdValgt: undefined,
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
              page: 1,
              lagretFilterIdValgt: undefined,
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
              page: 1,
              lagretFilterIdValgt: undefined,
            });
          }}
        />
      ))}
      {!tiltakstypeId &&
        filter.tiltakstyper.map((tiltakstype) => (
          <FilterTag
            key={tiltakstype}
            label={tiltakstyper.find((t) => tiltakstype === t.id)?.navn || tiltakstype}
            onClose={() => {
              setFilter({
                ...filter,
                tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
                page: 1,
                lagretFilterIdValgt: undefined,
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
              page: 1,
              lagretFilterIdValgt: undefined,
            });
          }}
        />
      ))}
      {(filter.personvernBekreftet === false || filter.personvernBekreftet === true) && (
        <FilterTag
          label={filter.personvernBekreftet ? "Personvern bekreftet" : "Personvern ikke bekreftet"}
          onClose={() => {
            setFilter({
              ...filter,
              personvernBekreftet: undefined,
              page: 1,
              lagretFilterIdValgt: undefined,
            });
          }}
        />
      )}
    </FilterTagsContainer>
  );
}
