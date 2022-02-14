import React from 'react';
import Tiltaksvariantoversikt from '../../components/tiltaksvariantoversikt/Tiltaksvariantoversikt';
import Sokefelt from '../../components/filtrering/Sokefelt';
import './TiltaksvariantOversikt.less';
import MainView from '../../layouts/MainView';
import useTiltaksvarianter from '../../hooks/tiltaksvariant/useTiltaksvarianter';
import { Loader } from '@navikt/ds-react';

const TiltaksvariantOversikt = () => {
  const { data, isFetching } = useTiltaksvarianter();

  return (
    <MainView
      title="Tiltaksvarianter"
      subTitle="Se en oversikt over alle nasjonale tiltaksvarianter"
      dataTestId="header-tiltaksvarianter"
    >
      <div className="tiltaksvarianter-oversikt-actions">
        <Sokefelt />
      </div>
      {isFetching && <Loader variant="neutral" size="2xlarge" />}
      {data && <Tiltaksvariantoversikt tiltaksvarianter={data} />}
    </MainView>
  );
};

export default TiltaksvariantOversikt;
