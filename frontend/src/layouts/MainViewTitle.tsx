import React from 'react';
import { Sidetittel, Systemtittel } from 'nav-frontend-typografi';
import './MainView.less';

interface MainViewTitleProps {
  title: string;
  subTitle?: string;
  dataTestId?: string;
}

function MainViewTitle({ title, subTitle, dataTestId }: MainViewTitleProps) {
  return (
    <div className="main-view__title">
      <Sidetittel data-testid={dataTestId}>{title}</Sidetittel>
      {subTitle && <Systemtittel>{subTitle}</Systemtittel>}
    </div>
  );
}

export default MainViewTitle;
