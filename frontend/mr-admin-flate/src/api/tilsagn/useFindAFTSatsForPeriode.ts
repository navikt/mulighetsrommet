import { AFTSats } from "@mr/api-client";
import { useAFTSatser } from "./useAFTSatser";

export function useFindAFTSatsForPeriode() {
  const { data: satser } = useAFTSatser();

  function findSats(start: Date): number | undefined {
    const filteredData =
      satser
        ?.filter((sats: AFTSats) => new Date(sats.startDato) <= start)
        ?.sort(
          (a: AFTSats, b: AFTSats) =>
            new Date(b.startDato).getTime() - new Date(a.startDato).getTime(),
        ) ?? [];

    return filteredData[0]?.belop;
  }

  return { findSats };
}
