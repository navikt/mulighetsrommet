import { useParams } from 'react-router-dom';

export function useHentFnrFraUrl() {
  const { fnr = 'undefined' } = useParams();
  return fnr;
}
