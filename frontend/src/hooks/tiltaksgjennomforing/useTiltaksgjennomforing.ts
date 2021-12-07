import { useQuery } from 'react-query';
import { QueryKeys } from '../../core/api/QueryKeys';
import TiltaksgjennomforingService from '../../core/api/TiltaksgjennomforingService';
import { Id } from '../../core/domain/Generic';
import { Tiltaksgjennomforing } from '../../core/domain/Tiltaksgjennomforing';

export default function useTiltaksgjennomforing(id: Id) {
  return useQuery<Tiltaksgjennomforing>([QueryKeys.Tiltaksgjennomforinger, id], () =>
    TiltaksgjennomforingService.getTiltaksgjennomforingById(id)
  );
}
