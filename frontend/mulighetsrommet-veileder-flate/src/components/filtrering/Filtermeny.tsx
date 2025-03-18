import { useRegioner } from "@/api/queries/useRegioner";
import { filterAccordionAtom } from "@/core/atoms";
import { useArbeidsmarkedstiltakFilter } from "@/hooks/useArbeidsmarkedstiltakFilter";
import { ApentForPamelding } from "@mr/api-client-v2";
import { FilterAccordionHeader, NavEnhetFilter } from "@mr/frontend-common";
import { addOrRemove } from "@mr/frontend-common/utils/utils";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { Accordion } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { FilterToggle } from "./FilterToggle";
import { InnsatsgruppeFilter } from "./InnsatsgruppeFilter";
import Sokefelt from "./Sokefelt";
import { Tiltakstypefilter } from "./Tiltakstypefilter";

export function Filtermeny() {
  const [filter, setFilter] = useArbeidsmarkedstiltakFilter();
  const [accordionsOpen, setAccordionsOpen] = useAtom(filterAccordionAtom);
  const { data: regioner } = useRegioner();

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
        <Tiltakstypefilter antallValgteTiltakstyper={filter.tiltakstyper.length} />
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
              navEnheter={filter.navEnheter}
              setNavEnheter={(navEnheter: string[]) =>
                setFilter({
                  ...filter,
                  navEnheter: regioner
                    .flatMap((region) => region.enheter)
                    .filter((enhet) => navEnheter.includes(enhet.enhetsnummer)),
                })
              }
              regioner={regioner ?? []}
            />
          </Accordion.Content>
        </Accordion.Item>
      </Accordion>
    </div>
  );
}
