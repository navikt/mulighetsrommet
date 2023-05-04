import { useAtom } from 'jotai';
import { Innsatsgruppe } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { tiltaksgjennomforingsfilter } from '../../atoms/atoms';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useHentBrukerdata } from './useHentBrukerdata';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';

export default function useTiltaksgjennomforinger() {
  const [filter] = useAtom(tiltaksgjennomforingsfilter);
  const brukerData = useHentBrukerdata();
  const fnr = useHentFnrFraUrl();

  return useQuery(QueryKeys.sanity.tiltaksgjennomforinger(brukerData.data, filter), () =>
    mulighetsrommetClient.sanity.getTiltaksgjennomforingForBruker({
      fnr,
      innsatsgruppe: filter.innsatsgruppe?.nokkel,
      sokestreng: filter.search,
      lokasjoner: filter.lokasjoner.map(({ tittel }) => tittel),
      tiltakstypeIder: filter.tiltakstyper.map(({ id }) => id),
    })
  );
}

export function utledInnsatsgrupperFraInnsatsgruppe(innsatsgruppe: string): Innsatsgruppe[] {
  switch (innsatsgruppe) {
    case 'STANDARD_INNSATS':
      return [Innsatsgruppe.STANDARD_INNSATS];
    case 'SITUASJONSBESTEMT_INNSATS':
      return [Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS];
    case 'SPESIELT_TILPASSET_INNSATS':
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
      ];
    case 'VARIG_TILPASSET_INNSATS':
      return [
        Innsatsgruppe.STANDARD_INNSATS,
        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
        Innsatsgruppe.VARIG_TILPASSET_INNSATS,
      ];
    default:
      return [];
  }
}
