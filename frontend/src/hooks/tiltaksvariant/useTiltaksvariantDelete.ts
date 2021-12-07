import { useMutation } from 'react-query';
import { useHistory } from 'react-router-dom';
import { toast } from 'react-toastify';
import TiltaksvariantService from '../../core/api/TiltaksvariantService';
import { Id } from '../../core/domain/Generic';

export default function useTiltaksvariantDelete(id: Id) {
  const history = useHistory();

  return useMutation(() => TiltaksvariantService.deleteTiltaksvariant(id), {
    onSuccess() {
      toast.success('Sletting vellykket!');

      history.replace(`/`);
    },
    onError() {
      toast.error('Sletting feilet.');
    },
  });
}
