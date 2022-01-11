import React from 'react';
import './MainView.less';
import { Heading } from '@navikt/ds-react';

interface MainViewTitleProps {
  title: string;
  subTitle?: string;
  dataTestId?: string;
}

function MainViewTitle({ title, subTitle, dataTestId }: MainViewTitleProps) {
  return (
    <div className="main-view__title">
      <Heading size="2xlarge" data-testid={dataTestId}>
        {title}
      </Heading>
      {subTitle && <Heading size="medium">{subTitle}</Heading>}
    </div>
  );
}

export default MainViewTitle;
