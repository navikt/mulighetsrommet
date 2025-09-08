import { useAvtalteSatser } from "@/api/tilsagn/useAvtalteSatser";

export function useFindAvtaltSats(avtaleId: string, periodeStart?: string | null) {
  const { data: satser } = useAvtalteSatser(avtaleId);

  return satser?.find((sats) => periodeStart && sats.gjelderFra <= periodeStart);
}
