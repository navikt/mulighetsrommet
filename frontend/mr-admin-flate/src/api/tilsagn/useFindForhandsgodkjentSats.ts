import { Tiltakskode } from "@mr/api-client";
import { useForhandsgodkjenteSatser } from "./useForhandsgodkjenteSatser";

export function useFindForhandsgodkjentSats(tiltakstype: Tiltakskode, periodeStart: string) {
  const { data: satser } = useForhandsgodkjenteSatser(tiltakstype);

  return satser?.find(
    (sats) => sats.periodeStart <= periodeStart && periodeStart <= sats.periodeSlutt,
  );
}
