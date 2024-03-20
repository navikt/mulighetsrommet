import { WritableAtom, useAtom } from "jotai";
import { NavEnhet, Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingFilter } from "../../api/atoms";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import { FilterTag } from "./FilterTag";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "../../utils/filterUtils";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilter,
    [newValue: TiltaksgjennomforingFilter],
    void
  >;
}

export function TiltaksgjennomforingFilterTags({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const { data: virksomheter } = useVirksomheter(VirksomhetTil.TILTAKSGJENNOMFORING);
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1,
  );

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
      {filter.search ? (
        <FilterTag
          label={`'${filter.search}'`}
          onClick={() => {
            setFilter({
              ...filter,
              search: "",
            });
          }}
        />
      ) : null}
      {filter.navEnheter
        ? filter.navEnheter.map((enhet: NavEnhet) => (
            <FilterTag
              key={enhet.enhetsnummer}
              label={enhet.navn}
              onClick={() => {
                setFilter({
                  ...filter,
                  navEnheter: addOrRemove(filter.navEnheter, enhet),
                });
              }}
            />
          ))
        : null}
      {filter.tiltakstyper
        ? filter.tiltakstyper.map((tiltakstype) => (
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
          ))
        : null}
      {filter.statuser
        ? filter.statuser.map((status) => (
            <FilterTag
              key={status}
              label={TILTAKSGJENNOMFORING_STATUS_OPTIONS.find((o) => status === o.value)?.label}
              onClick={() => {
                setFilter({
                  ...filter,
                  statuser: addOrRemove(filter.statuser, status),
                });
              }}
            />
          ))
        : null}
      {filter.arrangorOrgnr
        ? filter.arrangorOrgnr.map((orgNr) => (
            <FilterTag
              key={orgNr}
              label={virksomheter?.find((v) => v.organisasjonsnummer === orgNr)?.navn}
              onClick={() => {
                setFilter({
                  ...filter,
                  arrangorOrgnr: addOrRemove(filter.arrangorOrgnr, orgNr),
                });
              }}
            />
          ))
        : null}
    </div>
  );
}
