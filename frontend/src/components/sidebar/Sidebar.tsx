import React from 'react';
import { Heading, Panel } from '@navikt/ds-react';
import InnsatsgruppeFilter from '../filtrering/InnsatsgruppeFilter';
import './Sidebar.less';

interface InnsatsgruppeFilterProps {
  filter: number[];
  setFilter: (innsatsgrupper: number[]) => void;
}
const Sidebar = ({ filter, setFilter }: InnsatsgruppeFilterProps) => {
  return (
    <Panel border className="tiltakstype-oversikt__sidebar">
      <Heading size="large" level="2" className="sidebar__heading">
        Filter
      </Heading>
      <InnsatsgruppeFilter innsatsgruppefilter={filter} setInnsatsgruppefilter={setFilter} />
    </Panel>
  );
};

export default Sidebar;
