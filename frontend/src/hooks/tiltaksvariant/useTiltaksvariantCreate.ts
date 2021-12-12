import { useMutation } from 'react-query';
import { useHistory } from 'react-router-dom';
import { toast } from 'react-toastify';
import { MulighetsrommetService } from '../../api';

export default function useTiltaksvariantCreate() {
  const history = useHistory();

  return useMutation(MulighetsrommetService.createTiltaksvariant, {
    onSuccess(tiltaksvariant) {
      toast.success('Oppretting vellykket!');

      history.replace(`/tiltaksvarianter/${tiltaksvariant.id}`);
    },
    onError() {
      toast.error('Oppretting feilet.');
    },
  });
}
