import { useAvtalteSatser } from "@/api/tilsagn/useAvtalteSatser";

export function useFindAvtaltSats(avtaleId: string, periodeStart: string) {
  const { data: satser } = useAvtalteSatser(avtaleId);

  return satser?.find(
    (sats) => sats.periodeStart <= periodeStart && periodeStart <= sats.periodeSlutt,
  );
}
