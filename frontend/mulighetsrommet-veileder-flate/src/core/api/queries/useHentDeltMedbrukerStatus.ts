import { DelMedBruker } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { useHentFnrFraUrl } from '../../../hooks/useHentFnrFraUrl';
import { mulighetsrommetClient } from '../clients';
import { QueryKeys } from '../query-keys';
import { useGetTiltaksnummerFraUrl } from './useGetTiltaksnummerFraUrl';
import { useHentVeilederdata } from './useHentVeilederdata';

export function useHentDeltMedBrukerStatus() {
  const tiltaksnummer = useGetTiltaksnummerFraUrl();
  const { data: veilederData } = useHentVeilederdata();
  const bruker_fnr = useHentFnrFraUrl();
  const { data: sistDeltMedBruker, refetch } = useQuery<DelMedBruker>(
    [QueryKeys.DeltMedBrukerStatus, bruker_fnr, veilederData?.ident, tiltaksnummer],
    () =>
      mulighetsrommetClient.delMedBruker.getDelMedBruker({
        fnr: bruker_fnr,
        navident: veilederData?.ident,
        tiltaksnummer,
      })
  );

  async function lagreVeilederHarDeltTiltakMedBruker() {
    if (!veilederData?.ident) return;

    try {
      const res = await mulighetsrommetClient.delMedBruker.postDelMedBruker({
        requestBody: { bruker_fnr, navident: veilederData?.ident, tiltaksnummer },
      });

      if (!res.ok) {
        // TODO What to do?
        throw new Error('Klarte ikke lagre info om deling av tiltak');
      }
      const data = await res.json();
    } catch (error) {
      // TODO What to do? Er ikke kritisk om vi ikke får lagret det i databasen, bare litt kjipt.
    }
  }

  return { harDeltMedBruker: sistDeltMedBruker, lagreVeilederHarDeltTiltakMedBruker, refetch };
}
