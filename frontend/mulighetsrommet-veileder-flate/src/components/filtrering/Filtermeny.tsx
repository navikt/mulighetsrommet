import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Accordion } from "@navikt/ds-react";
import { ApentForInnsok } from "mulighetsrommet-api-client";
import {
  RegionMap,
  useArbeidsmarkedstiltakFilter,
  valgteEnhetsnumre,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { FilterToggle } from "./FilterToggle";
import styles from "./Filtermeny.module.scss";
import InnsatsgruppeFilter from "./InnsatsgruppeFilter";
import { NavEnhetFilter } from "./NavEnhetFilter";
import Sokefelt from "./Sokefelt";
import { Tiltakstypefilter } from "./Tiltakstypefilter";
import { FilterAccordionHeader } from "mulighetsrommet-frontend-common";

export const Filtermeny = () => {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();

  return (
    <div className={styles.tiltakstype_oversikt_filtermeny} id="tiltakstype_oversikt_filtermeny">
      <Sokefelt
        sokefilter={filter.search}
        setSokefilter={(search: string) => setFilter({ ...filter, search })}
      />
      <Accordion>
        <FilterToggle
          accordionHeader={<FilterAccordionHeader tittel="Åpent for innsøk" />}
          value={filter.apentForInnsok}
          onChange={(apentForInnsok) => {
            setFilter({ ...filter, apentForInnsok });
          }}
          venstreTekst="Åpent"
          hoyreTekst={
            <>
              <PadlockLockedFillIcon aria-hidden />
              Stengt
            </>
          }
          venstreValue={ApentForInnsok.APENT}
          midtValue={ApentForInnsok.APENT_ELLER_STENGT}
          hoyreValue={ApentForInnsok.STENGT}
          accordionIsOpenValue="apen-for-innsok"
        />
        <InnsatsgruppeFilter />
        <NavEnhetFilter
          regionMapFilter={filter.regionMap}
          setRegionMapFilter={(regionMap: RegionMap) => setFilter({ ...filter, regionMap })}
          antallValgteEnheter={valgteEnhetsnumre(filter).length}
        />
        <Tiltakstypefilter antallValgteTiltakstyper={filter.tiltakstyper.length} />
      </Accordion>
    </div>
  );
};
