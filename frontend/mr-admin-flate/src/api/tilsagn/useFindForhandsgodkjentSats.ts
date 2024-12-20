import { useAvtalteSatser } from "./useAvtalteSatser";

export function useFindForhandsgodkjentSats(avtaleId: string, periodeStart: string) {
  const { data: satser } = useAvtalteSatser(avtaleId);

  return satser?.find(
    (sats) => sats.periodeStart <= periodeStart && periodeStart <= sats.periodeSlutt,
  );
}
