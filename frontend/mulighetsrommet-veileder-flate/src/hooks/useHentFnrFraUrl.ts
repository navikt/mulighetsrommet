import { useParams } from 'react-router-dom';

export function useHentFnrFraUrl() {
  const { fnr } = useParams();
  return fnr;
}
