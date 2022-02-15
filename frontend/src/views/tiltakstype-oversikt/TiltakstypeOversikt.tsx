import React from 'react';
import Tiltakstypeoversikt from '../../components/tiltakstypeoversikt/Tiltakstypeoversikt';
import Sokefelt from '../../components/filtrering/Sokefelt';
import './TiltakstypeOversikt.less';
import MainView from '../../layouts/MainView';
import { Loader } from '@navikt/ds-react';
import useTiltakstyper from '../../hooks/tiltakstype/useTiltakstyper';

const TiltakstypeOversikt = () => {
  const { data, isFetching } = useTiltakstyper();

  return (
    <MainView
      title="Tiltakstyper"
      subTitle="Se en oversikt over alle nasjonale tiltakstyper"
      dataTestId="header-tiltakstyper"
    >
      <div className="tiltakstyper-oversikt-actions">
        <Sokefelt />
      </div>
      {isFetching && <Loader variant="neutral" size="2xlarge" />}
      {data && <Tiltakstypeoversikt tiltakstyper={data} />}
    </MainView>
  );
};

export default TiltakstypeOversikt;
