import React from 'react';
import Tiltaksvariantoversikt from '../../components/tiltaksvariantoversikt/Tiltaksvariantoversikt';
import Sokefelt from '../../components/filtrering/Sokefelt';
import './TiltaksvariantOversikt.less';
import MainView from '../../layouts/MainView';
import useTiltaksvarianter from '../../hooks/tiltaksvariant/useTiltaksvarianter';
import { ReactComponent as AddCircle } from '../../ikoner/AddCircle.svg';
import { Button, Loader } from '@navikt/ds-react';
import { useHistory } from 'react-router-dom';

const TiltaksvariantOversikt = () => {
  const { data, isFetching } = useTiltaksvarianter();
  const history = useHistory();

  return (
    <MainView
      title="Tiltaksvarianter"
      subTitle="Se en oversikt over alle nasjonale tiltaksvarianter"
      dataTestId="header-tiltaksvarianter"
    >
      <div className="tiltaksvarianter-oversikt-actions">
        <Sokefelt />
        <Button
          variant="primary"
          className="knapp opprett-ny-tiltaksvariant__knapp"
          data-testid="knapp_opprett-tiltaksvariant"
          onClick={() => history.push('/tiltaksvarianter/opprett')}
        >
          Opprett tiltaksvariant <AddCircle />
        </Button>
      </div>
      {isFetching && <Loader variant="neutral" size="2xlarge" />}
      {data && <Tiltaksvariantoversikt tiltaksvarianter={data} />}
    </MainView>
  );
};

export default TiltaksvariantOversikt;
