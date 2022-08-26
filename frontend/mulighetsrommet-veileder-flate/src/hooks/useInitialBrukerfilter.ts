import { useEffect } from 'react';
import { useErrorHandler } from 'react-error-boundary';
import { useHentBrukerdata } from '../core/api/queries/useHentBrukerdata';
import { useInnsatsgrupper } from '../core/api/queries/useInnsatsgrupper';
import { usePrepopulerFilter } from './usePrepopulerFilter';

export function useInitialBrukerfilter() {
  const maybeBrukerdata = useHentBrukerdata();
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  useErrorHandler(maybeBrukerdata?.error);
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const brukerdata = maybeBrukerdata?.data;

  useEffect(() => {
    if (brukerdata && innsatsgrupper) {
      forcePrepopulerFilter(true, innsatsgrupper, brukerdata);
    }
  }, [brukerdata, innsatsgrupper]);
}
