import { useAtom } from 'jotai';
import { Bruker } from 'mulighetsrommet-api-client';
import { useErrorHandler } from 'react-error-boundary';
import { Innsatsgruppe } from '../core/api/models';
import { useHentBrukerdata } from '../core/api/queries/useHentBrukerdata';
import { useInnsatsgrupper } from '../core/api/queries/useInnsatsgrupper';
import { tiltaksgjennomforingsfilter } from '../core/atoms/atoms';

export function usePrepopulerFilter() {
  const [filter, setFilter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerdata = useHentBrukerdata();
  useErrorHandler(brukerdata?.error);

  function forcePrepopulerFilter(
    resetFilterTilUtgangspunkt: boolean,
    innsatsgrupper?: Innsatsgruppe[],
    brukerdata?: Bruker
  ) {
    const matchedInnsatsgruppe = innsatsgrupper?.find(gruppe => gruppe.nokkel === brukerdata?.innsatsgruppe);
    if (matchedInnsatsgruppe) {
      const tiltakstyper = resetFilterTilUtgangspunkt ? [] : filter.tiltakstyper;
      const search = resetFilterTilUtgangspunkt ? '' : filter.search;
      const innsatsgrupper = resetFilterTilUtgangspunkt
        ? [{ id: matchedInnsatsgruppe._id, ...matchedInnsatsgruppe }]
        : [...filter.innsatsgrupper.filter(gruppe => gruppe.id !== matchedInnsatsgruppe._id)];
      setFilter({
        search,
        tiltakstyper,
        innsatsgrupper,
      });
    }
  }

  return { forcePrepopulerFilter };
}
