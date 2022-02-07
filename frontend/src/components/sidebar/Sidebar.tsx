import React from 'react';
import { Heading, Panel } from '@navikt/ds-react';
import InnsatsgruppeFilter from './filter/Innsatsgruppefilter';
import './Sidebar.less';

const Sidebar = () => {
  return (
    <Panel border className="tiltaksvariant-oversikt__sidebar">
      <Heading size="xlarge" level="2" className="sidebar__heading">
        Filter
      </Heading>
      <InnsatsgruppeFilter />
    </Panel>
  );
};

export default Sidebar;
