import { useAtom, WritableAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { AvtaleFilter } from "../../api/atoms";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import { FilterTag } from "./FilterTag";
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
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "start",
        alignItems: "center",
        flexWrap: "wrap",
        rowGap: "0.25rem",
      }}
    >
      {" "}
      {filter.sok && (
        <FilterTag
          label={`'${filter.sok}'`}
          onClick={() => {
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
          label={AVTALE_STATUS_OPTIONS.find((o) => status === o.value)?.label}
          onClick={() => {
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
          label={enheter?.find((e) => e.enhetsnummer === enhetsnummer)?.navn}
          onClick={() => {
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
            label={tiltakstyper?.data?.find((t) => tiltakstype === t.id)?.navn}
            onClick={() => {
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
          label={leverandorer?.find((l) => l.organisasjonsnummer === orgnr)?.navn}
          onClick={() => {
            setFilter({
              ...filter,
              leverandor: addOrRemove(filter.leverandor, orgnr),
            });
          }}
        />
      ))}
    </div>
  );
}
