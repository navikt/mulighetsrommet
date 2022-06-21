import React, { useEffect } from 'react';
import { Button } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { FAKE_DOOR, useFeatureToggles } from '../../api/feature-toggles';
import Filtermeny from '../../components/filtrering/Filtermeny';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingsTabell';
import FilterTags from '../../components/tags/Filtertags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import '../../layouts/TiltaksgjennomforingsHeader.less';
import Show from '../../utils/Show';
import './ViewTiltakstypeOversikt.less';
import FakeDoor from '../../components/fakedoor/FakeDoor';

const ViewTiltakstypeOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);

  const features = useFeatureToggles();
  const visFakeDoorFeature = features.isSuccess && features.data[FAKE_DOOR];

  //TODO fiks denne når vi får inn prefiltrering
  useEffect(() => {
    if (filter.tiltakstyper?.length === 0 && filter.innsatsgrupper?.length === 0) {
      setFilter(tiltaksgjennomforingsfilter.init);
    }
  }, [filter.tiltakstyper, filter.innsatsgrupper]);

  return (
    <>
      {!visFakeDoorFeature ? (
        <FakeDoor />
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
                    tiltakstyper: filter.tiltakstyper?.filter(tiltakstype => tiltakstype._id !== id),
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
            <TiltaksgjennomforingsTabell />
          </div>
        </div>
      )}
    </>
  );
};

export default ViewTiltakstypeOversikt;
