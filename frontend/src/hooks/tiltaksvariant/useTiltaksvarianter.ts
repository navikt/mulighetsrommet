import { useQuery } from 'react-query';
import { QueryKeys } from '../../core/api/QueryKeys';
import TiltaksvariantService from '../../core/api/TiltaksvariantService';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';

export default function useTiltaksvarianter() {
  return useQuery<Tiltaksvariant[]>(QueryKeys.Tiltaksvarianter, TiltaksvariantService.getAllTiltaksvarianter);
}
