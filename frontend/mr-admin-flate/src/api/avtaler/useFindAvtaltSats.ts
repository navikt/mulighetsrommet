import { useAvtalteSatser } from "@/api/avtaler/useAvtalteSatser";

export function useFindAvtaltSats(avtaleId: string, periodeStart?: string | null) {
  const { data: satser } = useAvtalteSatser(avtaleId);

  return satser?.findLast((sats) => periodeStart && periodeStart >= sats.gjelderFra);
}
