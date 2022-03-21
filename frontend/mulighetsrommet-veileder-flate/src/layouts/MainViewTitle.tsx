import React from 'react';
import './MainView.less';
import { Heading } from '@navikt/ds-react';
import { kebabCase } from '../utils/Utils';

interface MainViewTitleProps {
  title: string;
  arrangor?: string;
  tiltakstype: string;
}

function MainViewTitle({ title, arrangor, tiltakstype }: MainViewTitleProps) {
  return (
    <div className="main-view__title">
      <Heading level="1" size="xlarge" data-testid={`main-view-header_${kebabCase(title)}`}>
        {title}
      </Heading>
      <div className="main-view__subtitle-container">
        {arrangor && (
          <Heading level="2" size="xsmall">
            {arrangor}
          </Heading>
        )}
        <Heading level="2" size="xsmall">
          {tiltakstype}
        </Heading>
      </div>
    </div>
  );
}

export default MainViewTitle;
