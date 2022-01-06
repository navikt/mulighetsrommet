import { useMutation } from 'react-query';
import { useHistory } from 'react-router-dom';
import { toast } from 'react-toastify';
import { MulighetsrommetService } from '../../api';

export default function useTiltaksvariantDelete() {
  const history = useHistory();

  return useMutation(MulighetsrommetService.deleteTiltaksvariant, {
    onSuccess() {
      toast.success('Sletting vellykket!');

      history.replace(`/`);
    },
    onError() {
      toast.error('Sletting feilet.');
    },
  });
}
