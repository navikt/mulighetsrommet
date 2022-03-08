import React from 'react';
import './MainView.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';

interface MainViewTitleProps {
  title: string;
  subTitle?: string;
}

function MainViewTitle({ title, subTitle }: MainViewTitleProps) {
  return (
    <div className="main-view__title">
      <Heading level="1" size="xlarge" data-testid={`main-view-header_${kebabCase(title)}`}>
        {title}
      </Heading>
      {subTitle && (
        <Heading level="2" size="medium">
          {subTitle}
        </Heading>
      )}
    </div>
  );
}

export default MainViewTitle;
