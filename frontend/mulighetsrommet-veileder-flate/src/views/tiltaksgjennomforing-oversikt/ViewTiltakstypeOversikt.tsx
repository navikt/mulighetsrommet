import { Button } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { useErrorHandler } from 'react-error-boundary';
import Filtermeny from '../../components/filtrering/Filtermeny';
import { BrukersOppfolgingsenhet } from '../../components/oppfolgingsenhet/BrukerOppfolgingsenhet';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingsTabell';
import FilterTags from '../../components/tags/Filtertags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useInnsatsgrupper } from '../../core/api/queries/useInnsatsgrupper';
import { tiltaksgjennomforingsfilter, Tiltaksgjennomforingsfiltergruppe } from '../../core/atoms/atoms';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';
import '../../layouts/TiltaksgjennomforingsHeader.less';
import Show from '../../utils/Show';
import './ViewTiltakstypeOversikt.less';

const ViewTiltakstypeOversikt = () => {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  const brukerdata = useHentBrukerdata();
  const innsatsgrupper = useInnsatsgrupper();

  useErrorHandler(brukerdata?.error);

  const brukersInnsatsgruppeErIkkeValgt = (gruppe: Tiltaksgjennomforingsfiltergruppe) =>
    gruppe.nokkel !== brukerdata?.data?.innsatsgruppe;

  const skalResetteFilter =
    (filter.innsatsgrupper.length === 0 && !!brukerdata?.data?.innsatsgruppe) ||
    filter.search !== '' ||
    filter.tiltakstyper.length > 0 ||
    filter.innsatsgrupper.some(brukersInnsatsgruppeErIkkeValgt);

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
          <Show if={!innsatsgrupper.isLoading && skalResetteFilter}>
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
