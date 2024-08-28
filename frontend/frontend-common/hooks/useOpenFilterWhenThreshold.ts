import { useState, useLayoutEffect } from "react";

export function useOpenFilterWhenThreshold(
  thresholdInPx: number,
): [boolean, React.Dispatch<React.SetStateAction<boolean>>] {
  const [thresholdMet, setThresholdMet] = useState<boolean>(false);

  useLayoutEffect(() => {
    setThresholdMet(window.innerWidth > thresholdInPx);
  }, []);

  return [thresholdMet, setThresholdMet];
}
