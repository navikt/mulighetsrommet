import React, { FunctionComponent } from 'react';
import './MainView.less';
import MainViewTitle from './MainViewTitle';
import Tilbakeknapp from '../components/tilbakeknapp/Tilbakeknapp';

interface MainViewProps {
  title?: string;
  subTitle?: string;
  dataTestId?: string;
  tilbakelenke?: string;
}

const MainView: FunctionComponent<MainViewProps> = ({ title, subTitle, dataTestId, tilbakelenke, children }) => {
  return (
    <div className="main-view">
      <div className="main-view__header">
        {tilbakelenke && <Tilbakeknapp tilbakelenke={tilbakelenke} />}
        {title && <MainViewTitle title={title} subTitle={subTitle} dataTestId={dataTestId} />}
      </div>
      {children}
    </div>
  );
};

export default MainView;
