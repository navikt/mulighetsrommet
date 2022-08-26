import { useEffect } from 'react';
import { useErrorHandler } from 'react-error-boundary';
import { useHentBrukerdata } from '../core/api/queries/useHentBrukerdata';
import { useInnsatsgrupper } from '../core/api/queries/useInnsatsgrupper';
import { usePrepopulerFilter } from './usePrepopulerFilter';

export function useInitialBrukerfilter(fnr: string) {
  const brukerdata = useHentBrukerdata();
  const { forcePrepopulerFilter } = usePrepopulerFilter();
  useErrorHandler(brukerdata?.error);
  const { data: innsatsgrupper } = useInnsatsgrupper();
  const data = brukerdata?.data;

  useEffect(() => {
    if (data?.innsatsgruppe?.length !== 0 && innsatsgrupper) {
      forcePrepopulerFilter(true);
    }
  }, [data, innsatsgrupper, fnr]);
}
