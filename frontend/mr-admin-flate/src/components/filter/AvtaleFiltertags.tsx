import { useAtom, WritableAtom } from "jotai";
import { ArrangorTil } from "mulighetsrommet-api-client";
import { AvtaleFilter } from "@/api/atoms";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useArrangorer } from "@/api/arrangor/useArrangorer";
import { addOrRemove, avtaletypeTilTekst } from "../../utils/Utils";
import { AVTALE_STATUS_OPTIONS } from "../../utils/filterUtils";
import { Filtertag, FiltertagsContainer } from "mulighetsrommet-frontend-common";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
  filterOpen?: boolean;
}

export function AvtaleFiltertags({ filterAtom, tiltakstypeId, filterOpen }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);

  const { data: enheter } = useNavEnheter();
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: arrangorer } = useArrangorer(ArrangorTil.AVTALE);

  return (
    <FiltertagsContainer filterOpen={filterOpen}>
      {filter.sok && (
        <Filtertag
          label={filter.sok}
          onClose={() => {
            setFilter({
              ...filter,
              sok: "",
            });
          }}
        />
      )}
      {filter.statuser.map((status) => (
        <Filtertag
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
        <Filtertag
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
        <Filtertag
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
        <Filtertag
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
          <Filtertag
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
        <Filtertag
          key={id}
          label={arrangorer?.find((arrangor) => arrangor.id === id)?.navn ?? id}
          onClose={() => {
            setFilter({
              ...filter,
              arrangorer: addOrRemove(filter.arrangorer, id),
            });
          }}
        />
      ))}
    </FiltertagsContainer>
  );
}
