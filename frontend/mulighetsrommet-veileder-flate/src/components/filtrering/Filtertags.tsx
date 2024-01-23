import { ApentForInnsok } from "mulighetsrommet-api-client";
import { useHentBrukerdata } from "../../core/api/queries/useHentBrukerdata";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "../../hooks/useArbeidsmarkedstiltakFilter";
import { ErrorTag } from "../tags/ErrorTag";
import FilterTag from "../tags/FilterTag";
import styles from "./Filtertags.module.scss";
import { NavEnhetTag } from "./NavEnhetTag";

export function Filtertags() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const brukerdata = useHentBrukerdata();

  return (
    <div className={styles.filtertags} data-testid="filtertags">
      <NavEnhetTag />
      {!brukerdata.isLoading &&
        !brukerdata.data?.innsatsgruppe &&
        !brukerdata.data?.servicegruppe && (
          <ErrorTag
            innhold="Innsatsgruppe og servicegruppe mangler"
            title="Kontroller om brukeren er under oppfølging og finnes i Arena"
            dataTestId="alert-innsatsgruppe"
          />
        )}
      {filter.apentForInnsok !== ApentForInnsok.APENT_ELLER_STENGT && (
        <FilterTag
          options={[
            {
              id: filter.apentForInnsok,
              tittel: filter.apentForInnsok === ApentForInnsok.APENT ? "Åpent" : "Stengt",
            },
          ]}
          handleClick={() =>
            setFilter({
              ...filter,
              apentForInnsok: ApentForInnsok.APENT_ELLER_STENGT,
            })
          }
        />
      )}
      {filter.innsatsgruppe && <FilterTag options={[filter.innsatsgruppe]} />}
      <FilterTag
        options={filter.tiltakstyper}
        handleClick={(id: string) =>
          setFilter({
            ...filter,
            tiltakstyper: filter.tiltakstyper?.filter(
              (tiltakstype: ArbeidsmarkedstiltakFilterGruppe<string>) => tiltakstype.id !== id,
            ),
          })
        }
      />
      {filter.search && (
        <FilterTag
          options={[{ id: "search", tittel: `'${filter.search}'` }]}
          handleClick={() => setFilter({ ...filter, search: "" })}
        />
      )}
    </div>
  );
}
