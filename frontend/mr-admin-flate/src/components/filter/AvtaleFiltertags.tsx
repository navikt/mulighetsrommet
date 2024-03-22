import { useAtom, WritableAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { AvtaleFilter } from "../../api/atoms";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
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
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
      kategori: undefined,
    },
    1,
  );
  const { data: leverandorer } = useVirksomheter(VirksomhetTil.AVTALE);

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
      {filter.leverandor.map((orgnr) => (
        <Filtertag
          key={orgnr}
          label={leverandorer?.find((l) => l.organisasjonsnummer === orgnr)?.navn || orgnr}
          onClose={() => {
            setFilter({
              ...filter,
              leverandor: filter.leverandor.filter((l) => l !== orgnr),
            });
          }}
        />
      ))}
    </FiltertagsContainer>
  );
}
