import { Accordion } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { tiltaksgjennomforingsfilter } from "../../core/atoms/atoms";
import styles from "./Filtermeny.module.scss";
import InnsatsgruppeFilter from "./InnsatsgruppeFilter";
import Sokefelt from "./Sokefelt";
import { Tiltakstypefilter } from "./Tiltakstypefilter";
import { FilterToggle } from "./FilterToggle";
import { PadlockLockedFillIcon } from "@navikt/aksel-icons";
import { ApentForInnsok, HarDeltMedBruker } from "mulighetsrommet-api-client";

const Filtermeny = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  return (
    <div className={styles.tiltakstype_oversikt_filtermeny} id="tiltakstype_oversikt_filtermeny">
      <Sokefelt
        sokefilter={filter.search}
        setSokefilter={(search: string) => setFilter({ ...filter, search })}
      />
      <Accordion>
        {/*<ApentForInnsokToggle*/}
        {/*  value={filter.apentForInnsok}*/}
        {/*  onChange={(apentForInnsok) => {*/}
        {/*    setFilter({ ...filter, apentForInnsok });*/}
        {/*  }}*/}
        {/*/>*/}
        <FilterToggle
          accordionHeader="Åpent for innsøk"
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
        <FilterToggle
          accordionHeader="Har delt med bruker"
          value={filter.harDeltMedBruker}
          onChange={(harDeltMedBruker) => {
            setFilter({ ...filter, harDeltMedBruker });
          }}
          venstreTekst={"Har delt"}
          hoyreTekst={"Har ikke delt"}
          venstreValue={HarDeltMedBruker.HAR_DELT}
          midtValue={HarDeltMedBruker.HAR_OG_HAR_IKKE_DELT}
          hoyreValue={HarDeltMedBruker.HAR_IKKE_DELT}
          accordionIsOpenValue={"har-delt-med-bruker"}
        />
        <InnsatsgruppeFilter />
        <Tiltakstypefilter />
      </Accordion>
    </div>
  );
};

export default Filtermeny;
