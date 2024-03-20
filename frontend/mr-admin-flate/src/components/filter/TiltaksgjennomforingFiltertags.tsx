import { useAtom, WritableAtom } from "jotai";
import { Tiltakstypestatus, VirksomhetTil } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingFilter } from "../../api/atoms";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { addOrRemove } from "../../utils/Utils";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { TILTAKSGJENNOMFORING_STATUS_OPTIONS } from "../../utils/filterUtils";
import { Filtertag } from "../../../../frontend-common/components/filter/filtertag/Filtertag";
import { FiltertagsContainer } from "../../../../frontend-common/components/filter/filtertag/FiltertagsContainer";

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
  const { data: enheter } = useNavEnheter();
  const { data: virksomheter } = useVirksomheter(VirksomhetTil.TILTAKSGJENNOMFORING);
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1,
  );

  return (
    <FiltertagsContainer filterOpen={filterOpen}>
      {filter.search ? (
        <Filtertag
          options={[{ id: "search", tittel: `'${filter.search}'` }]}
          onClose={() => {
            setFilter({
              ...filter,
              search: "",
            });
          }}
        />
      ) : null}
      {filter.navRegioner
        ? filter.navRegioner.map((enhetsnummer) => (
            <Filtertag
              key={enhetsnummer}
              options={[
                {
                  id: enhetsnummer,
                  tittel:
                    enheter?.find((e) => e.enhetsnummer === enhetsnummer)?.navn || enhetsnummer,
                },
              ]}
              onClose={() => {
                setFilter({
                  ...filter,
                  navRegioner: addOrRemove(filter.navRegioner, enhetsnummer),
                });
              }}
            />
          ))
        : null}
      {filter.navEnheter
        ? filter.navEnheter.map((enhetsnummer) => (
            <Filtertag
              key={enhetsnummer}
              options={[
                {
                  id: enhetsnummer,
                  tittel:
                    enheter?.find((e) => e.enhetsnummer === enhetsnummer)?.navn || enhetsnummer,
                },
              ]}
              onClose={() => {
                setFilter({
                  ...filter,
                  navEnheter: addOrRemove(filter.navEnheter, enhetsnummer),
                });
              }}
            />
          ))
        : null}
      {filter.tiltakstyper
        ? filter.tiltakstyper.map((tiltakstype) => (
            <Filtertag
              key={tiltakstype}
              options={[
                {
                  id: tiltakstype,
                  tittel:
                    tiltakstyper?.data?.find((t) => tiltakstype === t.id)?.navn || tiltakstype,
                },
              ]}
              onClose={() => {
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
            <Filtertag
              key={status}
              options={[
                {
                  id: status,
                  tittel:
                    TILTAKSGJENNOMFORING_STATUS_OPTIONS.find((o) => status === o.value)?.label ||
                    status,
                },
              ]}
              onClose={() => {
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
            <Filtertag
              key={orgNr}
              options={[
                {
                  id: orgNr,
                  tittel: virksomheter?.find((v) => v.organisasjonsnummer === orgNr)?.navn || orgNr,
                },
              ]}
              onClose={() => {
                setFilter({
                  ...filter,
                  arrangorOrgnr: addOrRemove(filter.arrangorOrgnr, orgNr),
                });
              }}
            />
          ))
        : null}
    </FiltertagsContainer>
  );
}
