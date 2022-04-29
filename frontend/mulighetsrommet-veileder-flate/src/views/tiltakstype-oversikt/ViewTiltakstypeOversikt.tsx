import React, { useEffect } from 'react';
import './ViewTiltakstypeOversikt.less';
import '../../layouts/MainView.less';
import { Alert, Button, Loader } from '@navikt/ds-react';
import Filtermeny from '../../components/filtrering/Filtermeny';
import TiltakstypeTabell from '../../components/tabell/TiltakstypeTabell';
import { useAtom } from 'jotai';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { FAKE_DOOR, useFeatureToggles } from '../../api/feature-toggles';
import FilterTags from '../../components/tags/Filtertags';
import useTiltaksgjennomforinger from '../../hooks/tiltaksgjennomforing/useTiltaksgjennomforinger';
import Show from '../../utils/Show';

const ViewTiltakstypeOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  const features = useFeatureToggles();
  const visFakeDoorFeature = features.isSuccess && features.data[FAKE_DOOR];

  const { data, isFetching, isError } = useTiltaksgjennomforinger(filter);

  //TODO fiks denne når vi får inn prefiltrering
  useEffect(() => {
    if (filter.tiltakstyper?.length === 0 && filter.innsatsgrupper?.length === 0) {
      setFilter(tiltaksgjennomforingsfilter.init);
    }
  }, [filter.tiltakstyper, filter.innsatsgrupper]);

  return (
    <>
      {visFakeDoorFeature ? (
        <Alert variant="info">Her kommer det noe spennende!</Alert>
      ) : (
        <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
          <Filtermeny />
          <div className="filtercontainer">
            <div className="filtertags" data-testid="filtertags">
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
            <Show if={filter !== tiltaksgjennomforingsfilter.init}>
              <div className="tilbakestill-filter-knapp">
                <Button
                  size="small"
                  variant="secondary"
                  onClick={() => setFilter(tiltaksgjennomforingsfilter.init)}
                  data-testid="knapp_tilbakestill-filter"
                >
                  Tilbakestill filter
                </Button>
              </div>
            </Show>
          </div>
          <div className="tiltakstype-oversikt__tiltak">
            {isFetching && !data && <Loader variant="neutral" size="2xlarge" />}
            {data && <TiltakstypeTabell tiltaksgjennomforingsliste={data} />}
            {isError && <Alert variant="error">Det har skjedd en feil</Alert>}
          </div>
        </div>
      )}
    </>
  );
};

export default ViewTiltakstypeOversikt;
