import { useParams } from 'react-router-dom';

export function useGetTiltaksgjennomforingIdFraUrl() {
  const { tiltaksnummer = '' } = useParams();
  return tiltaksnummer;
}
