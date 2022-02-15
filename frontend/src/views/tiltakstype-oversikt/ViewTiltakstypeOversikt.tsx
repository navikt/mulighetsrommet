import React from 'react';
import Sokefelt from '../../components/filtrering/Sokefelt';
import './ViewTiltakstypeOversikt.less';
import '../../layouts/MainView.less';
import MainView from '../../layouts/MainView';
import { Alert, BodyShort, Loader } from '@navikt/ds-react';
import Sidebar from '../../components/sidebar/Sidebar';
import useTiltakstyper from '../../hooks/tiltakstype/useTiltakstyper';
import { tiltakstypefilter } from '../../api/atoms/atoms';
import { useAtom } from 'jotai';
import TiltakstypeTabell from '../../components/tabell/TiltakstypeTabell';

const ViewTiltakstypeOversikt = () => {
  const [filter, setFilter] = useAtom(tiltakstypefilter);
  const { data, isFetching, isError } = useTiltakstyper(filter); //isLoading vs isFetching?

  return (
    <MainView
      title="Tiltakstyper"
      subTitle="Se en oversikt over alle nasjonale tiltakstyper"
      dataTestId="header-tiltakstyper"
      contentClassName="tiltakstype-oversikt"
    >
      <Sidebar
        filter={filter.innsatsgrupper ?? []}
        setFilter={innsatsgrupper => setFilter({ ...filter, innsatsgrupper })}
      />
      <div className="tiltakstype-oversikt__sokefelt">
        <Sokefelt
          sokefilter={filter.search ?? ''}
          setSokefilter={(search: string) => setFilter({ ...filter, search })}
        />
      </div>
      <div className="tiltakstype-oversikt__tiltak">
        <BodyShort>
          Viser {data?.length} av {data?.length} tiltak
        </BodyShort>
        {isFetching && !data && <Loader variant="neutral" size="2xlarge" />}
        {data && <TiltakstypeTabell tiltakstypeliste={data} />}
        {isError && <Alert variant="error">En feil oppstod. Vi har problemer med å hente tiltakstypene.</Alert>}
      </div>
    </MainView>
  );
};

export default ViewTiltakstypeOversikt;
