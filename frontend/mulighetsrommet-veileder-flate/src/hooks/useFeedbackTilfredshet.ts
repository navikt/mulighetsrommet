import { useQuery } from 'react-query';

export function useFeedbackTilfredshet(tilfredshet: number) {
  const { data } = useQuery(['feedbackTilfredshet', tilfredshet], () => tilfredshet, {
    enabled: tilfredshet > 0,
  });

  return { data };
}
