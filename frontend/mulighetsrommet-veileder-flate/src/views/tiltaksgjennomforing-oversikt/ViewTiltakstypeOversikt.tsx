import React from 'react';
import { Alert, Button, Tag } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import Filtermeny from '../../components/filtrering/Filtermeny';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingsTabell';
import FilterTags from '../../components/tags/Filtertags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { tiltaksgjennomforingsfilter, Tiltaksgjennomforingsfiltergruppe } from '../../core/atoms/atoms';
import '../../layouts/TiltaksgjennomforingsHeader.less';
import Show from '../../utils/Show';
import './ViewTiltakstypeOversikt.less';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { kebabCase } from '../../utils/Utils';
import { useErrorHandler } from 'react-error-boundary';

function BrukersOppfolgingsenhet() {
  const brukerdata = useHentBrukerdata();
  const brukersOppfolgingsenhet = brukerdata?.data?.oppfolgingsenhet?.navn;

  if (brukerdata?.isLoading) {
    return null;
  }

  return brukersOppfolgingsenhet ? (
    <Tag
      className={'nav-enhet-tag'}
      key={'navenhet'}
      variant={brukersOppfolgingsenhet ? 'info' : 'error'}
      size="small"
      data-testid={`${kebabCase('filtertag_navenhet')}`}
      title="Brukers oppfølgingsenhet"
    >
      {brukersOppfolgingsenhet}
    </Tag>
  ) : (
    <Alert key="alert-navenhet" data-testid="alert-navenhet" size="small" variant="error">
      Klarte ikke hente brukers oppfølgingsenhet
    </Alert>
  );
}

const ViewTiltakstypeOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const brukerdata = useHentBrukerdata();
  useErrorHandler(brukerdata?.error);
  const brukersInnsatsgruppeErIkkeValgt = (gruppe: Tiltaksgjennomforingsfiltergruppe) =>
    gruppe.nokkel !== brukerdata?.data?.innsatsgruppe;

  return (
    <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <Filtermeny />
      <div className="filtercontainer">
        <div className="filtertags" data-testid="filtertags">
          <BrukersOppfolgingsenhet />
          <FilterTags
            options={filter.innsatsgrupper!}
            handleClick={(id: string) => {
              setFilter({
                ...filter,
                innsatsgrupper: [...filter.innsatsgrupper?.filter(innsatsgruppe => innsatsgruppe.id !== id)],
              });
            }}
          />
          <FilterTags
            options={filter.tiltakstyper!}
            handleClick={(id: string) =>
              setFilter({
                ...filter,
                tiltakstyper: filter.tiltakstyper?.filter(tiltakstype => tiltakstype.id !== id),
              })
            }
          />
          <SearchFieldTag />
          <Show
            if={
              filter.innsatsgrupper.length === 0 ||
              filter.innsatsgrupper.some(brukersInnsatsgruppeErIkkeValgt) ||
              filter.search !== '' ||
              filter.tiltakstyper.length > 0
            }
          >
            <Button
              size="small"
              variant="tertiary"
              className="tilbakestill-filter-knapp"
              onClick={() => {
                setFilter(RESET);
                forcePrepopulerFilter(true);
              }}
              data-testid="knapp_tilbakestill-filter"
            >
              Tilbakestill filter
            </Button>
          </Show>
        </div>
      </div>
      <div className="tiltakstype-oversikt__tiltak">
        <TiltaksgjennomforingsTabell />
      </div>
    </div>
  );
};

export default ViewTiltakstypeOversikt;
