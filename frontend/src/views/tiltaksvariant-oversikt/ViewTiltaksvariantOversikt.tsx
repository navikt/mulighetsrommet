import React from 'react';
import Tiltaksvariantoversikt from '../../components/tiltaksvariantoversikt/Tiltaksvariantoversikt';
import Sokefelt from '../../components/filtrering/Sokefelt';
import './TiltaksvariantOversikt.less';
import '../../layouts/MainView.less';
import MainView from '../../layouts/MainView';
import useTiltaksvarianter from '../../hooks/tiltaksvariant/useTiltaksvarianter';
import { Loader } from '@navikt/ds-react';
import { useHistory } from 'react-router-dom';
import Sidebar from '../../components/sidebar/Sidebar';

const TiltaksvariantOversikt = () => {
  const { data, isFetching } = useTiltaksvarianter();
  const history = useHistory();

  return (
    <MainView
      title="Tiltaksvarianter"
      subTitle="Se en oversikt over alle nasjonale tiltaksvarianter"
      dataTestId="header-tiltaksvarianter"
      className="tiltaksvariant-oversikt"
    >
      <Sidebar />
      <div className="tiltaksvariant-oversikt__actions">
        <Sokefelt />
        {/*<Button*/}
        {/*  variant="primary"*/}
        {/*  className="knapp opprett-ny-tiltaksvariant__knapp"*/}
        {/*  data-testid="knapp_opprett-tiltaksvariant"*/}
        {/*  onClick={() => history.push('/tiltaksvarianter/opprett')}*/}
        {/*>*/}
        {/*  Opprett tiltaksvariant <AddCircle />*/}
        {/*</Button>*/}
      </div>
      {isFetching && <Loader variant="neutral" size="2xlarge" />}
      {data && <Tiltaksvariantoversikt tiltaksvarianter={data} />}
    </MainView>
  );
};

export default TiltaksvariantOversikt;
