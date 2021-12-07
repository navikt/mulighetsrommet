import { useQuery } from 'react-query';
import { QueryKeys } from '../../core/api/QueryKeys';
import TiltaksvariantService from '../../core/api/TiltaksvariantService';
import { Id } from '../../core/domain/Generic';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';

export default function useTiltaksvariant(id: Id) {
  return useQuery<Tiltaksvariant>([QueryKeys.Tiltaksvarianter, id], () =>
    TiltaksvariantService.getTiltaksvariantById(id)
  );
}
