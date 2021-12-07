import { useQuery } from 'react-query';
import { QueryKeys } from '../../core/api/QueryKeys';
import TiltaksgjennomforingService from '../../core/api/TiltaksgjennomforingService';
import { Tiltaksgjennomforing } from '../../core/domain/Tiltaksgjennomforing';

export default function useTiltaksgjennomforinger() {
  return useQuery<Tiltaksgjennomforing[]>(
    QueryKeys.Tiltaksgjennomforinger,
    TiltaksgjennomforingService.getAllTiltaksgjennomforinger
  );
}
