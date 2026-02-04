import { filterAccordionAtom } from "@/core/atoms";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { ApentForPamelding } from "@api-client";
import { FilterAccordionHeader } from "@mr/frontend-common";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Accordion } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { FilterToggle } from "./FilterToggle";
import { InnsatsgruppeFilter } from "./InnsatsgruppeFilter";
import Sokefelt from "./Sokefelt";
import { TiltakstypeFilter } from "./TiltakstypeFilter";
import { NavEnhetFilter } from "@/components/filtrering/NavEnhetFilter";

export function FilterMenu() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);

  return (
    <div data-testid="filtertabs">
      <Sokefelt
        sokefilter={filter.search}
        setSokefilter={(search: string) => setFilter({ ...filter, search })}
      />
      <Accordion>
        <FilterToggle
          accordionHeader={<FilterAccordionHeader tittel="Åpent for påmelding" />}
          value={filter.apentForPamelding}
          onChange={(apentForPamelding) => {
            setFilter({ ...filter, apentForPamelding });
          }}
          venstreTekst="Åpent"
          hoyreTekst={
            <>
              <PadlockLockedFillIcon aria-hidden />
              Stengt
            </>
          }
          venstreValue={ApentForPamelding.APENT}
          midtValue={ApentForPamelding.APENT_ELLER_STENGT}
          hoyreValue={ApentForPamelding.STENGT}
          accordionIsOpenValue="apen-for-pamelding"
        />
        <InnsatsgruppeFilter />
        <Accordion.Item open={accordionsOpen.includes("tiltakstyper")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "tiltakstyper")]);
            }}
          >
            <FilterAccordionHeader
              tittel="Tiltakstype"
              antallValgteFilter={filter.tiltakstyper.length}
            />
          </Accordion.Header>
          <Accordion.Content data-testid="filter_accordioncontent_brukers-enhet">
            <TiltakstypeFilter
              value={filter.tiltakstyper}
              onChange={(tiltakstyper) => setFilter({ ...filter, tiltakstyper })}
            />
          </Accordion.Content>
        </Accordion.Item>
        <Accordion.Item open={accordionsOpen.includes("brukers-enhet")}>
          <Accordion.Header
            onClick={() => {
              setAccordionsOpen([...addOrRemove(accordionsOpen, "brukers-enhet")]);
            }}
            data-testid="filter_accordionheader_brukers-enhet"
          >
            <FilterAccordionHeader
              tittel="Nav-enhet"
              antallValgteFilter={filter.navEnheter.length}
            />
          </Accordion.Header>
          <Accordion.Content data-testid="filter_accordioncontent_brukers-enhet">
            <NavEnhetFilter
              value={filter.navEnheter}
              onChange={(navEnheter) => setFilter({ ...filter, navEnheter })}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
