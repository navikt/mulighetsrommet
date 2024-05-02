import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Accordion } from "@navikt/ds-react";
import { ApentForInnsok, NavEnhet } from "mulighetsrommet-api-client";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { FilterToggle } from "./FilterToggle";
import styles from "./Filtermeny.module.scss";
import { InnsatsgruppeFilter } from "./InnsatsgruppeFilter";
import Sokefelt from "./Sokefelt";
import { Tiltakstypefilter } from "./Tiltakstypefilter";
import { useAtom } from "jotai";
import { filterAccordionAtom } from "@/core/atoms";
import { useRegioner } from "@/api/queries/useRegioner";
import { addOrRemove } from "mulighetsrommet-frontend-common/utils/utils";
import { NavEnhetFilter } from "mulighetsrommet-frontend-common";
import { FilterAccordionHeader } from "mulighetsrommet-frontend-common";

export const Filtermeny = () => {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);
  const { data: regioner } = useRegioner();

  return (
    <div className={styles.tiltakstype_oversikt_filtermeny} data-testid="filtertabs">
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
        <Accordion.Item open={accordionsOpen.includes("brukers-enhet")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "brukers-enhet")]);
            }}
            data-testid="filter_accordionheader_brukers-enhet"
          >
            <FilterAccordionHeader
              tittel="NAV-enhet"
              antallValgteFilter={filter.navEnheter.length}
            />
          </Accordion.Header>
          <Accordion.Content data-testid="filter_accordioncontent_brukers-enhet">
            <NavEnhetFilter
              navEnheter={filter.navEnheter}
              setNavEnheter={(navEnheter: NavEnhet[]) => setFilter({ ...filter, navEnheter })}
              regioner={regioner}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Tiltakstypefilter antallValgteTiltakstyper={filter.tiltakstyper.length} />
      </Accordion>
    </div>
  );
};
