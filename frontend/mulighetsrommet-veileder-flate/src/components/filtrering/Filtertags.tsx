import { BrukersOppfolgingsenhet } from '../oppfolgingsenhet/BrukerOppfolgingsenhet';
import FilterTag from '../tags/FilterTag';
import SearchFieldTag from '../tags/SearchFieldTag';
import Show from '../../utils/Show';
import { Alert, Button } from '@navikt/ds-react';
import { RESET } from 'jotai/utils';
import { useAtom } from 'jotai';
import { tiltaksgjennomforingsfilter } from '../../core/atoms/atoms';
import { usePrepopulerFilter } from '../../hooks/usePrepopulerFilter';
import { useInnsatsgrupper } from '../../core/api/queries/useInnsatsgrupper';
import { useHentBrukerdata } from '../../core/api/queries/useHentBrukerdata';
import { useErrorHandler } from 'react-error-boundary';
import { InnsatsgruppeNokler } from '../../core/api/models';
import styles from './Filtertags.module.scss';

export function Filtertags() {
  const brukerdata = useHentBrukerdata();
  useErrorHandler(brukerdata?.error);
  const brukersInnsatsgruppeErIkkeValgt = (innsatsgruppe?: InnsatsgruppeNokler) => {
    return innsatsgruppe !== brukerdata?.data?.innsatsgruppe;
  };

  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const { forcePrepopulerFilter } = usePrepopulerFilter();

  const innsatsgrupper = useInnsatsgrupper();

  const skalResetteFilter =
    filter.innsatsgruppe! === undefined ||
    brukersInnsatsgruppeErIkkeValgt(filter.innsatsgruppe.nokkel) ||
    filter.search !== '' ||
    filter.tiltakstyper.length > 0 ||
    filter.tiltaksgruppe.length > 0 ||
    filter.lokasjoner.length > 0;

  return (
    <div className={styles.filtertags} data-testid="filtertags">
      <BrukersOppfolgingsenhet />
      {brukerdata.data?.innsatsgruppe && (
        <Alert
          title="Kontroller om brukeren er under oppfølging og finnes i Arena"
          key="alert-innsatsgruppe"
          data-testid="alert-innsatsgruppe"
          size="small"
          variant="error"
        >
          Innsatsgruppe mangler
        </Alert>
      )}
      {filter.innsatsgruppe && (
        <FilterTag
          options={[filter.innsatsgruppe]}
          handleClick={() => {
            setFilter({
              ...filter,
              innsatsgruppe: undefined,
            });
          }}
        />
      )}
      <FilterTag
        options={filter.tiltakstyper!}
        handleClick={(id: string) =>
          setFilter({
            ...filter,
            tiltakstyper: filter.tiltakstyper?.filter(tiltakstype => tiltakstype.id !== id),
          })
        }
      />
      <FilterTag
        options={filter.tiltaksgruppe!}
        handleClick={(id: string) =>
          setFilter({
            ...filter,
            tiltaksgruppe: filter.tiltaksgruppe?.filter(gruppe => gruppe.id !== id),
          })
        }
      />
      <FilterTag
        options={filter.lokasjoner!}
        handleClick={(id: string) =>
          setFilter({
            ...filter,
            lokasjoner: filter.lokasjoner?.filter(gruppe => gruppe.id !== id),
          })
        }
      />
      <SearchFieldTag />
      <Show if={!innsatsgrupper.isLoading && skalResetteFilter}>
        <Button
          size="small"
          variant="tertiary"
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
  );
}
