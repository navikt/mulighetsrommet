import { useParams } from 'react-router-dom';

export function useHentFnrFraUrl() {
  const { fnr = '12345678910' } = useParams();
  return fnr;
}
