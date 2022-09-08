import { useQuery } from 'react-query';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';

interface Historikk {
  id: string;
  fnr: string;
  fraDato: Date;
  tilDato: Date;
  status: Deltakerstatus;
  tiltaksnummer: string;
  tiltaksnavn: string;
  tiltakstype: string;
  arrangor: string;
}

export type Deltakerstatus = 'VENTER' | 'AVSLUTTET' | 'DELTAR' | 'IKKE_AKTUELL';

// TODO Type opp retur-verdi for hook
export function useHentHistorikk(prefetch: boolean = true) {
  const fnr = useHentFnrFraUrl();
  return useQuery<Historikk[], any>(
    [QueryKeys.Historikk, fnr],
    () => mulighetsrommetClient.historikk.hentHistorikkForBruker({ fnr }),
    { enabled: prefetch }
  );
}
