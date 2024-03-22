import { useAtom, WritableAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingFilter } from "../../api/atoms";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "../../utils/filterUtils";
import { NavEnhetFiltertag } from "mulighetsrommet-frontend-common";
import { Filtertag, FiltertagsContainer } from "mulighetsrommet-frontend-common";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilter,
    [newValue: TiltaksgjennomforingFilter],
    void
  >;
  filterOpen?: boolean;
}

export function TiltaksgjennomforingFiltertags({ filterAtom, filterOpen }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const { data: virksomheter } = useVirksomheter(VirksomhetTil.TILTAKSGJENNOMFORING);
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1,
  );

  return (
    <FiltertagsContainer filterOpen={filterOpen}>
      {filter.search && (
        <Filtertag
          label={filter.search}
          onClose={() => {
            setFilter({
              ...filter,
              search: "",
            });
          }}
        />
      )}
      {filter.navEnheter.length > 0 && (
        <NavEnhetFiltertag
          navEnheter={filter.navEnheter}
          onClose={() => setFilter({ ...filter, navEnheter: [] })}
        />
      )}
      {filter.tiltakstyper.map((tiltakstype) => (
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
      {filter.statuser.map((status) => (
        <Filtertag
          key={status}
          label={
            TILTAKSGJENNOMFORING_STATUS_OPTIONS.find((o) => status === o.value)?.label || status
          }
          onClose={() => {
            setFilter({
              ...filter,
              statuser: addOrRemove(filter.statuser, status),
            });
          }}
        />
      ))}
      {filter.visMineGjennomforinger && (
        <Filtertag
          label="Mine gjennomføringer"
          onClose={() => {
            setFilter({
              ...filter,
              visMineGjennomforinger: false,
            });
          }}
        />
      )}
      {filter.arrangorOrgnr.map((orgNr) => (
        <Filtertag
          key={orgNr}
          label={virksomheter?.find((v) => v.organisasjonsnummer === orgNr)?.navn || orgNr}
          onClose={() => {
            setFilter({
              ...filter,
              arrangorOrgnr: addOrRemove(filter.arrangorOrgnr, orgNr),
            });
          }}
        />
      ))}
    </FiltertagsContainer>
  );
}
