import { useParams } from 'react-router-dom';

export function useHentFnrFraUrl() {
  const { fnr } = useParams<{ fnr: string }>();

  if (!fnr) {
    return window.location.pathname.substring(1, window.location.pathname.lastIndexOf('/')); // Hent ut fnr fra pathname
  }

  return fnr;
}
