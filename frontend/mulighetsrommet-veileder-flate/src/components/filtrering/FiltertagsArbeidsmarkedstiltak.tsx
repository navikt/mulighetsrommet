import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  ArbeidsmarkedstiltakFilterGruppe,
  useArbeidsmarkedstiltakFilter,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import FilterTag from "../tags/FilterTag";
import styles from "./Filtertags.module.scss";
import { NavEnhetTag } from "./NavEnhetTag";

export function Filtertags() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <div className={styles.filtertags} data-testid="filtertags">
      <NavEnhetTag />
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
