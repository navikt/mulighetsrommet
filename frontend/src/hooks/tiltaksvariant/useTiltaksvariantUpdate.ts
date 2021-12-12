import { useMutation } from 'react-query';
import { useHistory } from 'react-router-dom';
import { toast } from 'react-toastify';
import { MulighetsrommetService } from '../../api';

export default function useTiltaksvariantUpdate() {
  const history = useHistory();

  return useMutation(MulighetsrommetService.updateTiltaksvariant, {
    onSuccess(tiltaksvariant) {
      toast.success('Endring vellykket!');

      history.replace(`/tiltaksvarianter/${tiltaksvariant.id}`);
    },
    onError() {
      toast.error('Endring feilet.');
    },
  });
}
