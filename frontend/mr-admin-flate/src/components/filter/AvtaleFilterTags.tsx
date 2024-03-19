import { useAtom, WritableAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { AvtaleFilter } from "../../api/atoms";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import { FilterTag } from "../../../../frontend-common/components/filtertag/Filtertag";
import { FilterTagsContainer } from "../../../../frontend-common/components/filtertag/FilterTagsContainer";
import { AVTALE_STATUS_OPTIONS } from "../../utils/filterUtils";

interface Props {
  filterAtom: WritableAtom<AvtaleFilter, [newValue: AvtaleFilter], void>;
  tiltakstypeId?: string;
}

export function AvtaleFilterTags({ filterAtom, tiltakstypeId }: Props) {
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
    <FilterTagsContainer>
      {filter.sok && (
        <FilterTag
          options={[{ id: "search", tittel: `'${filter.sok}'` }]}
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
          options={[
            {
              id: status,
              tittel: AVTALE_STATUS_OPTIONS.find((o) => status === o.value)?.label || status,
            },
          ]}
          onClose={() => {
            setFilter({
              ...filter,
              statuser: addOrRemove(filter.statuser, status),
            });
          }}
        />
      ))}
      {filter.navRegioner.map((enhetsnummer) => (
        <FilterTag
          key={enhetsnummer}
          options={[
            {
              id: enhetsnummer,
              tittel: enheter?.find((e) => e.enhetsnummer === enhetsnummer)?.navn || enhetsnummer,
            },
          ]}
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
            options={[
              {
                id: tiltakstype,
                tittel: tiltakstyper?.data?.find((t) => tiltakstype === t.id)?.navn || tiltakstype,
              },
            ]}
            onClose={() => {
              setFilter({
                ...filter,
                tiltakstyper: addOrRemove(filter.tiltakstyper, tiltakstype),
              });
            }}
          />
        ))}
      {filter.leverandor.map((orgnr) => (
        <FilterTag
          key={orgnr}
          options={[
            {
              id: orgnr,
              tittel: leverandorer?.find((l) => l.organisasjonsnummer === orgnr)?.navn || orgnr,
            },
          ]}
          onClose={() => {
            setFilter({
              ...filter,
              leverandor: addOrRemove(filter.leverandor, orgnr),
            });
          }}
        />
      ))}
    </FilterTagsContainer>
  );
}
