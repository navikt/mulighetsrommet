import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { avtaletypeTilTekst } from "@/utils/Utils";
import { AVTALE_STATUS_OPTIONS } from "@/utils/filterUtils";
import { FilterTag, FilterTagsContainer } from "@mr/frontend-common";
import { AvtaleFilterType } from "@/pages/avtaler/filter";
import { ArrangorKobling } from "@tiltaksadministrasjon/api-client";

interface Props {
  filter: AvtaleFilterType;
  updateFilter: (values: Partial<AvtaleFilterType>) => void;
  tiltakstypeId?: string;
  filterOpen: boolean;
  setTagsHeight: (height: number) => void;
}

export function AvtaleFilterTags({
  filter,
  updateFilter,
  tiltakstypeId,
  filterOpen,
  setTagsHeight,
}: Props) {
  const { data: enheter } = useNavEnheter();
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: arrangorer } = useArrangorer(ArrangorKobling.AVTALE, {
    pageSize: 10000,
  });

  return (
    <FilterTagsContainer filterOpen={filterOpen} setTagsHeight={setTagsHeight}>
      {filter.sok && (
        <FilterTag
          label={`Søkt på: '${filter.sok}'`}
          onClose={() => {
            updateFilter({
              sok: "",
              page: 1,
            });
          }}
        />
      )}
      {filter.statuser.map((status) => (
        <FilterTag
          key={status}
          label={AVTALE_STATUS_OPTIONS.find((o) => status === o.value)?.label || status}
          onClose={() => {
            updateFilter({
              statuser: addOrRemove(filter.statuser, status),
              page: 1,
            });
          }}
        />
      ))}
      {filter.avtaletyper.map((avtaletype) => (
        <FilterTag
          key={avtaletype}
          label={avtaletypeTilTekst(avtaletype)}
          onClose={() => {
            updateFilter({
              avtaletyper: addOrRemove(filter.avtaletyper, avtaletype),
              page: 1,
            });
          }}
        />
      ))}
      {filter.visMineAvtaler && (
        <FilterTag
          label="Mine avtaler"
          onClose={() => {
            updateFilter({
              visMineAvtaler: false,
              page: 1,
            });
          }}
        />
      )}
      {filter.navRegioner.map((enhetsnummer) => (
        <FilterTag
          key={enhetsnummer}
          label={enheter.find((e) => e.enhetsnummer === enhetsnummer)?.navn || enhetsnummer}
          onClose={() => {
            updateFilter({
              navRegioner: addOrRemove(filter.navRegioner, enhetsnummer),
              page: 1,
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
              updateFilter({
                tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
                page: 1,
              });
            }}
          />
        ))}
      {filter.arrangorer.map((id) => (
        <FilterTag
          key={id}
          label={arrangorer?.data.find((arrangor) => arrangor.id === id)?.navn ?? id}
          onClose={() => {
            updateFilter({
              arrangorer: addOrRemove(filter.arrangorer, id),
              page: 1,
            });
          }}
        />
      ))}
      {(filter.personvernBekreftet === false || filter.personvernBekreftet === true) && (
        <FilterTag
          label={filter.personvernBekreftet ? "Personvern bekreftet" : "Personvern ikke bekreftet"}
          onClose={() => {
            updateFilter({
              personvernBekreftet: undefined,
              page: 1,
            });
          }}
        />
      )}
    </FilterTagsContainer>
  );
}
