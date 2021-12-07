import React from 'react';
import Tiltaksvariantoversikt from '../../components/tiltaksvariantoversikt/Tiltaksvariantoversikt';
import Sokefelt from '../../components/filtrering/Sokefelt';
import './TiltaksvariantOversikt.less';
import MainView from '../../layouts/MainView';
import Link from '../../components/link/Link';
import useTiltaksvarianter from '../../hooks/tiltaksvariant/useTiltaksvarianter';
import NavFrontendSpinner from 'nav-frontend-spinner';

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
        <Link
          to="/tiltaksvarianter/opprett"
          className="knapp opprett-ny-tiltaksvariant__knapp"
          data-testid="knapp_opprett-tiltaksvariant"
        >
          Opprett tiltaksvariant
        </Link>
      </div>
      <div>{isFetching ? <NavFrontendSpinner /> : <Tiltaksvariantoversikt tiltaksvarianter={data} />}</div>
    </MainView>
  );
};

export default TiltaksvariantOversikt;
