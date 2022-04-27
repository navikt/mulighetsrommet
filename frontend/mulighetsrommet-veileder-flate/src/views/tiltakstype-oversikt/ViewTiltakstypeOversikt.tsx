import React from 'react';
import './ViewTiltakstypeOversikt.less';
import '../../layouts/MainView.less';
import { Alert, Loader } from '@navikt/ds-react';
import Filtermeny from '../../components/filtrering/Filtermeny';
import TiltakstypeTabell from '../../components/tabell/TiltakstypeTabell';
import { useAtom } from 'jotai';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { FAKE_DOOR, useFeatureToggles } from '../../api/feature-toggles';
import FilterTags from '../../components/tags/Filtertags';
import useTiltaksgjennomforinger from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforinger';

const ViewTiltakstypeOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  const features = useFeatureToggles();
  const visFakeDoorFeature = features.isSuccess && features.data[FAKE_DOOR];

  const { data, isFetching, isError } = useTiltaksgjennomforinger(filter);

  return (
    <>
      {visFakeDoorFeature ? (
        <Alert variant="info">
          Ååååja, så du tror at hvis du trykker på en knapp så skjer det ting automatisk, du da?
        </Alert>
      ) : (
        <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
          <Filtermeny />
          <div className="filtercontainer">
            <div className="filtertags">
              <FilterTags
                options={filter.innsatsgrupper!}
                handleClick={(id: number) =>
                  setFilter({
                    ...filter,
                    innsatsgrupper: filter.innsatsgrupper?.filter(innsatsgruppe => innsatsgruppe.id !== id),
                  })
                }
              />
              <FilterTags
                options={filter.tiltakstyper!}
                handleClick={(id: number) =>
                  setFilter({
                    ...filter,
                    tiltakstyper: filter.tiltakstyper?.filter(tiltakstype => tiltakstype.id !== id),
                  })
                }
              />
              <SearchFieldTag />
            </div>
          </div>
          <div className="tiltakstype-oversikt__tiltak">
            {isFetching && !data && <Loader variant="neutral" size="2xlarge" />}
            {data && <TiltakstypeTabell tiltaksgjennomforingsliste={data} />}
            {isError && <Alert variant="error">En feil oppstod. Vi har problemer med å hente tiltakstypene.</Alert>}
          </div>
        </div>
      )}
    </>
  );
};

export default ViewTiltakstypeOversikt;
