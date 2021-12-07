import { useMutation } from 'react-query';
import { useHistory } from 'react-router-dom';
import { toast } from 'react-toastify';
import TiltaksvariantService from '../../core/api/TiltaksvariantService';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';

export default function useTiltaksvariantCreate() {
  const history = useHistory();

  return useMutation((tiltaksvariant: Tiltaksvariant) => TiltaksvariantService.postTiltaksvariant(tiltaksvariant), {
    onSuccess(tiltaksvariant) {
      toast.success('Oppretting vellykket!');

      history.replace(`/tiltaksvarianter/${tiltaksvariant.id}`);
    },
    onError() {
      toast.error('Oppretting feilet.');
    },
  });
}
