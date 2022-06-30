import React from 'react';
import { Button } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { FAKE_DOOR, useFeatureToggles } from '../../api/feature-toggles';
import Filtermeny from '../../components/filtrering/Filtermeny';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingsTabell';
import FilterTags from '../../components/tags/Filtertags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { initialTiltaksgjennomforingsfilter, tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import '../../layouts/TiltaksgjennomforingsHeader.less';
import Show from '../../utils/Show';
import './ViewTiltakstypeOversikt.less';
import FakeDoor from '../../components/fakedoor/FakeDoor';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';

const ViewTiltakstypeOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { forcePrepopulerFilter } = usePrepopulerFilter();

  const features = useFeatureToggles();
  const visFakeDoorFeature = features.isSuccess && features.data[FAKE_DOOR];

  return (
    <>
      {visFakeDoorFeature ? (
        <FakeDoor />
      ) : (
        <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
          <Filtermeny />
          <div className="filtercontainer">
            <div className="filtertags" data-testid="filtertags">
              <FilterTags
                options={filter.innsatsgrupper!}
                handleClick={(id: string) =>
                  setFilter({
                    ...filter,
                    innsatsgrupper: filter.innsatsgrupper?.filter(innsatsgruppe => innsatsgruppe.id !== id),
                  })
                }
              />
              <FilterTags
                options={filter.tiltakstyper!}
                handleClick={(id: string) =>
                  setFilter({
                    ...filter,
                    tiltakstyper: filter.tiltakstyper?.filter(tiltakstype => tiltakstype.id !== id),
                  })
                }
              />
              <SearchFieldTag />
            </div>
            <Show if={filter.innsatsgrupper.length > 1 || filter.tiltakstyper.length > 0 || filter.search !== ''}>
              <div className="tilbakestill-filter-knapp">
                <Button
                  size="small"
                  variant="secondary"
                  onClick={() => {
                    setFilter(RESET);
                    forcePrepopulerFilter(true);
                  }}
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
