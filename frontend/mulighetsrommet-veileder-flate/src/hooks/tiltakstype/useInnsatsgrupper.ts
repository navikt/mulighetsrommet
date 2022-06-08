import { useQuery } from 'react-query';
import { MulighetsrommetService, Innsatsgruppe } from 'mulighetsrommet-api-client';
import { QueryKeys } from '../../core/api/QueryKeys';
import { client } from '../../sanityClient';

export function useInnsatsgrupper() {
  return useQuery<Innsatsgruppe[]>(
    QueryKeys.Innsatsgrupper,
    MulighetsrommetService.getInnsatsgrupper
    //   () =>
    // client.fetch(`*[_type == "tiltakstype"]{innsatsgruppe}`)
  );
}
