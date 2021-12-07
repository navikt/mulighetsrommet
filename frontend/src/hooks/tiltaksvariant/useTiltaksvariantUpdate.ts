import { useMutation } from 'react-query';
import { useHistory } from 'react-router-dom';
import { toast } from 'react-toastify';
import TiltaksvariantService from '../../core/api/TiltaksvariantService';
import { Id } from '../../core/domain/Generic';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';

export default function useTiltaksvariantUpdate(id: Id) {
  const history = useHistory();

  return useMutation((tiltaksvariant: Tiltaksvariant) => TiltaksvariantService.putTiltaksvariant(tiltaksvariant), {
    onSuccess() {
      toast.success('Endring vellykket!');

      history.replace(`/tiltaksvarianter/${id}`);
    },
    onError() {
      toast.error('Endring feilet.');
    },
  });
}
