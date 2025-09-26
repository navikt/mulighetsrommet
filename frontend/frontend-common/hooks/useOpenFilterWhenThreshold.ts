import { Dispatch, SetStateAction, useState, useLayoutEffect } from "react";

export function useOpenFilterWhenThreshold(
  thresholdInPx: number,
): [boolean, Dispatch<SetStateAction<boolean>>] {
  const [thresholdMet, setThresholdMet] = useState<boolean>(false);

  useLayoutEffect(() => {
    setThresholdMet(window.innerWidth > thresholdInPx);
  }, [thresholdInPx]);

  return [thresholdMet, setThresholdMet];
}
