import React, { FunctionComponent } from 'react';
import './MainView.less';
import MainViewTitle from './MainViewTitle';
import Tilbakeknapp from '../components/tilbakeknapp/Tilbakeknapp';
import classNames from 'classnames';

interface MainViewProps {
  title?: string;
  subTitle?: string;
  dataTestId?: string;
  tilbakelenke?: string;
  contentClassName?: string;
}

const MainView: FunctionComponent<MainViewProps> = ({
  title,
  subTitle,
  dataTestId,
  tilbakelenke,
  children,
  contentClassName,
}) => {
  return (
    <div className="main-view">
      <div className="main-view__header">
        {tilbakelenke && <Tilbakeknapp tilbakelenke={tilbakelenke} />}
        {title && <MainViewTitle title={title} subTitle={subTitle} dataTestId={dataTestId} />}
      </div>
      <div className={classNames('main-view__content', contentClassName)}>{children}</div>
    </div>
  );
};

export default MainView;
