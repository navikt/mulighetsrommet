import { Heading } from '@navikt/ds-react';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';
import { FilterForIndividueltEllerGruppetiltak } from './FilterForIndividueltEllerGruppetiltak';
import './Filtermeny.less';
import { Fritekstfilter } from './Fritekstfilter';
import InnsatsgruppeFilter from './InnsatsgruppeFilter';
import { Tiltakstypefilter } from './Tiltakstypefilter';
import { LokasjonFilter } from "./LokasjonFilter";

const Filtermeny = () => {
  usePrepopulerFilter();

  return (
    <div className="tiltakstype-oversikt__filtermeny">
      <Heading size="medium" level="1" className="filtermeny__heading" role="heading">
        Filter
      </Heading>
      <Fritekstfilter />
      <InnsatsgruppeFilter />
      <Tiltakstypefilter />
      <FilterForIndividueltEllerGruppetiltak />
      <LokasjonFilter />
    </div>
  );
};

export default Filtermeny;
