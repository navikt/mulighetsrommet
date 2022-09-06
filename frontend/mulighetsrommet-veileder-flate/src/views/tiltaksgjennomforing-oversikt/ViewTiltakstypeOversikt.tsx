import { Button } from '@navikt/ds-react';
import { useAtom } from 'jotai';
import { RESET } from 'jotai/utils';
import { useErrorHandler } from 'react-error-boundary';
import Filtermeny from '../../components/filtrering/Filtermeny';
import { HistorikkButton } from '../../components/historikk/HistorikkButton';
import { BrukersOppfolgingsenhet } from '../../components/oppfolgingsenhet/BrukerOppfolgingsenhet';
import TiltaksgjennomforingsTabell from '../../components/tabell/TiltaksgjennomforingsTabell';
import FilterTags from '../../components/tags/Filtertags';
import SearchFieldTag from '../../components/tags/SearchFieldTag';
import { InnsatsgruppeNokler } from '../../core/api/models';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useInnsatsgrupper } from '../../core/api/queries/useInnsatsgrupper';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
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

  const brukersInnsatsgruppeErIkkeValgt = (innsatsgruppe?: InnsatsgruppeNokler) => {
    return innsatsgruppe !== brukerdata?.data?.innsatsgruppe;
  };

  const skalResetteFilter =
    filter.innsatsgruppe! === undefined ||
    brukersInnsatsgruppeErIkkeValgt(filter.innsatsgruppe.nokkel) ||
    filter.search !== '' ||
    filter.tiltakstyper.length > 0;

  return (
    <div className="tiltakstype-oversikt" id="tiltakstype-oversikt" data-testid="tiltakstype-oversikt">
      <Filtermeny />
      <div className="filtercontainer">
        <div className="filtertags" data-testid="filtertags">
          <BrukersOppfolgingsenhet />
          {filter.innsatsgruppe && (
            <FilterTags
              options={[filter.innsatsgruppe]}
              handleClick={() => {
                setFilter({
                  ...filter,
                  innsatsgruppe: undefined,
                });
              }}
            />
          )}
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
          <HistorikkButton />
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
