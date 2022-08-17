import { useParams } from 'react-router-dom';

export function useGetTiltaksnummerFraUrl() {
  const { tiltaksnummer = '' } = useParams();
  return tiltaksnummer;
}
